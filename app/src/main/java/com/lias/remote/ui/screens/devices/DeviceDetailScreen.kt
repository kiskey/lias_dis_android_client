// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt
// Version: 11.0.0
//
// Purpose:
//   Authoritative device inspection/control.
//
// Batch 11:
//   - No raw pol_pause_* lookup.
//   - Pause / Extend / Resume / Cancel driven only by EffectiveStatus.
//   - Server expiry rendered as a locally ticking countdown.
//   - Pause and Extend are visually distinguished by reason_tag.
//   - Effective-policy source is shown.
//   - Infrastructure immunity retained.
//   - Deep-link/not-found handling from Batch 7 retained.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.DeviceIdentityFormatter
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.TemporaryAccessKind
import com.lias.remote.core.models.temporaryAccessKind
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.util.EffectiveAccessFormatter
import com.lias.remote.repositories.SyncState
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.ScreenStateTone
import com.lias.remote.ui.components.ScreenStateView
import com.lias.remote.ui.components.StaleDataNotice
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.formatTemporaryDuration
import com.lias.remote.ui.components.rememberTemporaryMinutesLeft
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone

@Composable
fun DeviceDetailScreen(
    pdid: String,
    viewModel: LiasViewModel,
    onBack: () -> Unit
) {

    val state by
        viewModel.state
            .collectAsState()

    val device =
        state.devices
            .find {
                it.pdid ==
                    pdid
            }

    val scrollState =
        rememberLazyListState()

    if (
        device == null
    ) {

        HigLargeTitleScaffold(
            title =
                "Device",
            scrollState =
                scrollState,
            navLeading = {

                HigTextButton(
                    text =
                        "‹ Devices",
                    onClick =
                        onBack
                )
            }
        ) { padding ->

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            padding
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                when (
                    val sync =
                        state.syncState
                ) {

                    SyncState.Idle,
                    SyncState.Loading -> {

                        ScreenStateView(
                            title =
                                "Loading Device",
                            message =
                                "Waiting for the LIAS device inventory."
                        )
                    }

                    is SyncState.Failed -> {

                        ScreenStateView(
                            title =
                                "Unable to Load Device",
                            message =
                                sync.message,
                            actionText =
                                "Try Again",
                            onAction =
                                viewModel::refresh,
                            tone =
                                ScreenStateTone.ERROR
                        )
                    }

                    else -> {

                        ScreenStateView(
                            title =
                                "Device Not Found",
                            message =
                                "This device is no longer present in the current LIAS inventory.",
                            actionText =
                                "Back to Devices",
                            onAction =
                                onBack
                        )
                    }
                }
            }
        }

        return
    }

    DeviceDetailContent(
        device =
            device,
        viewModel =
            viewModel,
        onBack =
            onBack
    )
}

@Composable
private fun DeviceDetailContent(
    device: Device,
    viewModel: LiasViewModel,
    onBack: () -> Unit
) {

    val state by
        viewModel.state
            .collectAsState()

    val scrollState =
        rememberLazyListState()

    val status =
        viewModel
            .effectiveStatusFor(
                device.pdid
            )

    val presentation =
        EffectiveAccessFormatter
            .present(
                status
            )

    val temporaryKind =
        status
            ?.temporaryAccessKind
            ?: TemporaryAccessKind.NONE

    val temporaryMinutes =
        rememberTemporaryMinutesLeft(
            status?.activeExtension
        )

    val isInfrastructure =
        status?.source ==
            "infrastructure" ||
            device.safeTags
                .contains(
                    "infrastructure"
                )

    var logs by
        remember(
            device.pdid
        ) {
            mutableStateOf<
                List<FlowLog>
            >(
                emptyList()
            )
        }

    var isLoadingLogs by
        remember(
            device.pdid
        ) {
            mutableStateOf(
                true
            )
        }

    var logError by
        remember(
            device.pdid
        ) {
            mutableStateOf<String?>(
                null
            )
        }

    var showExtendSheet by
        remember {
            mutableStateOf(
                false
            )
        }

    var showPauseSheet by
        remember {
            mutableStateOf(
                false
            )
        }

    var showUserAssignmentSheet by
        remember {
            mutableStateOf(
                false
            )
        }

    var showRenameDialog by
        remember {
            mutableStateOf(
                false
            )
        }

    val assignedUser =
        state.users.find {
            it.id ==
                device.userID
        }

    val identityPresentation =
        remember(
            device
        ) {
            DeviceIdentityFormatter
                .present(
                    device
                )
        }

    LaunchedEffect(
        device.pdid
    ) {

        isLoadingLogs =
            true

        logError =
            null

        when (
            val result =
                viewModel.getDeviceLogs(
                    device.pdid
                )
        ) {

            is ApiResult.Success -> {
                logs =
                    result.data
            }

            is ApiResult.AuthenticationError -> {
                logError =
                    result.message
            }

            is ApiResult.HttpError -> {
                logError =
                    result.message
            }

            is ApiResult.ConflictError -> {
                logError =
                    result.message
            }

            is ApiResult.NetworkError -> {
                logError =
                    result.cause
                        .message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to load activity."
            }

            is ApiResult.SerializationError -> {
                logError =
                    "LIAS returned invalid activity data."
            }
        }

        isLoadingLogs =
            false
    }

    HigLargeTitleScaffold(
        title =
            device.displayName,
        scrollState =
            scrollState,
        navLeading = {

            HigTextButton(
                text =
                    "‹ Devices",
                onClick =
                    onBack
            )
        },
        navTrailing = {

            HigTextButton(
                text =
                    "Refresh",
                onClick =
                    viewModel::refresh
            )
        }
    ) { padding ->

        LazyColumn(
            state =
                scrollState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                padding
        ) {

            if (
                state.syncState is
                SyncState.Stale
            ) {

                item {

                    StaleDataNotice(
                        message =
                            (
                                state.syncState
                                    as SyncState.Stale
                                ).message,
                        onRefresh =
                            viewModel::refresh
                    )
                }
            }

            item {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    16.dp,
                                vertical =
                                    12.dp
                            ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    68.dp
                                )
                                .background(
                                    LiasThemeColors.blue,
                                    RoundedCornerShape(
                                        17.dp
                                    )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        CupertinoIcon(
                            imageVector =
                                CupertinoIcons
                                    .Outlined
                                    .Iphone,
                            contentDescription =
                                null,
                            tint =
                                Color.White,
                            modifier =
                                Modifier.size(
                                    38.dp
                                )
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )

                    CupertinoText(
                        text =
                            device.displayName,
                        style =
                            HigTypography.title1,
                        fontWeight =
                            FontWeight.ExtraBold,
                        color =
                            LiasThemeColors.label
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    CupertinoText(
                        text =
                            identityPresentation
                                .subtitle,
                        style =
                            HigTypography.body,
                        color =
                            if (
                                device.online
                            ) {
                                LiasThemeColors.green
                            } else {
                                LiasThemeColors
                                    .tertiaryLabel
                            }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )

                    StatusPill(
                        text =
                            detailStatusText(
                                presentation.title,
                                temporaryKind,
                                temporaryMinutes,
                                isInfrastructure
                            ),
                        tone =
                            when {

                                isInfrastructure ->
                                    PillTone.INFO

                                temporaryKind ==
                                    TemporaryAccessKind.PAUSE ->
                                    PillTone.PAUSED

                                temporaryKind ==
                                    TemporaryAccessKind.EXTEND ->
                                    PillTone.ALLOWED

                                presentation.isBlocked ->
                                    PillTone.BLOCKED

                                presentation.isAllowed ->
                                    PillTone.ALLOWED

                                else ->
                                    PillTone.INFO
                            }
                    )

                    presentation.detail
                        ?.let { detail ->

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        6.dp
                                    )
                            )

                            CupertinoText(
                                text =
                                    detail,
                                style =
                                    HigTypography.caption,
                                color =
                                    LiasThemeColors
                                        .secondaryLabel
                            )
                        }

                    Spacer(
                        modifier =
                            Modifier.height(
                                16.dp
                            )
                    )

                    DeviceActionRow(
                        status =
                            status,
                        presentation =
                            presentation,
                        temporaryKind =
                            temporaryKind,
                        isInfrastructure =
                            isInfrastructure,
                        onPause = {
                            showPauseSheet =
                                true
                        },
                        onResume = {
                            viewModel
                                .unpauseDeviceInternet(
                                    device.pdid
                                )
                        },
                        onExtend = {
                            showExtendSheet =
                                true
                        },
                        onCancelExtension = {
                            viewModel
                                .cancelDeviceExtension(
                                    device.pdid
                                )
                        },
                        onRename = {
                            showRenameDialog =
                                true
                        }
                    )
                }
            }

            if (
                temporaryKind !=
                TemporaryAccessKind.NONE
            ) {

                item {
                    ListSectionHeader(
                        "Temporary Override"
                    )
                }

                item {

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {

                        GroupedListRow(
                            primaryText =
                                if (
                                    temporaryKind ==
                                    TemporaryAccessKind.PAUSE
                                ) {
                                    "Internet Paused"
                                } else {
                                    "Extended Access"
                                },
                            secondaryText =
                                when {
                                    temporaryMinutes ==
                                        null ->
                                        "Server-managed temporary override"

                                    temporaryMinutes <=
                                        0 ->
                                        "Ending now"

                                    else ->
                                        "${formatTemporaryDuration(temporaryMinutes)} remaining"
                                },
                            showDivider =
                                true
                        )

                        GroupedListRow(
                            primaryText =
                                "Managed By",
                            secondaryText =
                                "LIAS Server",
                            showDivider =
                                true
                        )

                        GroupedListRow(
                            primaryText =
                                "Expires At",
                            secondaryText =
                                status?.activeExtension
                                    ?.expiresAt
                                    ?.ifBlank {
                                        "Unavailable"
                                    }
                                    ?: "Unavailable"
                        )
                    }
                }
            }

            item {
                ListSectionHeader(
                    "Identity"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    GroupedListRow(
                        primaryText =
                            "Identity Status",
                        secondaryText =
                            DeviceIdentityFormatter
                                .identitySummary(
                                    device
                                ),
                        trailingContent = {

                            CupertinoText(
                                text =
                                    "${identityPresentation.confidencePercent}%",
                                style =
                                    HigTypography.subheadline,
                                color =
                                    LiasThemeColors
                                        .secondaryLabel
                            )
                        },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Hostname",
                        secondaryText =
                            device.hostname
                                .ifBlank {
                                    device.canonicalHostname
                                }
                                .ifBlank {
                                    "Unavailable"
                                },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "IP Address",
                        secondaryText =
                            device.currentIP
                                .ifBlank {
                                    "Unavailable"
                                },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "MAC Address",
                        secondaryText =
                            device.currentMAC
                                .ifBlank {
                                    "Unavailable"
                                },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Vendor",
                        secondaryText =
                            device.vendor
                                .ifBlank {
                                    device.manufacturer
                                }
                                .ifBlank {
                                    "Unknown"
                                },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Effective Source",
                        secondaryText =
                            EffectiveAccessFormatter
                                .sourceDescription(
                                    status?.source
                                        .orEmpty()
                                )
                                ?: "Checking",
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Assigned User",
                        secondaryText =
                            assignedUser?.name
                                ?: "Unassigned",
                        onClick = {
                            showUserAssignmentSheet =
                                true
                        }
                    )
                }
            }

            item {
                ListSectionHeader(
                    "Activity"
                )
            }

            item {

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp
                        )
                ) {

                    when {

                        isLoadingLogs -> {

                            GroupedListRow(
                                primaryText =
                                    "Loading Activity",
                                secondaryText =
                                    "Requesting recent LIAS events."
                            )
                        }

                        logError !=
                            null -> {

                            GroupedListRow(
                                primaryText =
                                    "Activity Unavailable",
                                secondaryText =
                                    logError
                            )
                        }

                        logs.isEmpty() -> {

                            GroupedListRow(
                                primaryText =
                                    "No Recent Activity",
                                secondaryText =
                                    "No recent activity was returned for this device."
                            )
                        }

                        else -> {

                            logs.forEachIndexed {
                                    index,
                                    log ->

                                val blocked =
                                    log.action
                                        .equals(
                                            "block",
                                            true
                                        )

                                GroupedListRow(
                                    primaryText =
                                        log.timestamp
                                            .ifBlank {
                                                "Activity"
                                            },
                                    secondaryText =
                                        if (
                                            log.bytes >
                                            0
                                        ) {
                                            "${log.bytes} bytes"
                                        } else {
                                            null
                                        },
                                    trailingContent = {

                                        StatusPill(
                                            text =
                                                log.action
                                                    .ifBlank {
                                                        "Event"
                                                    },
                                            tone =
                                                if (
                                                    blocked
                                                ) {
                                                    PillTone.BLOCKED
                                                } else {
                                                    PillTone.ALLOWED
                                                }
                                        )
                                    },
                                    showDivider =
                                        index <
                                            logs.lastIndex
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (
        showExtendSheet
    ) {

        ExtendAccessSheet(
            targetLabel =
                device.displayName,
            targetSubtitle =
                device.currentIP
                    .ifBlank {
                        device.pdid
                    },
            currentExtension =
                status
                    ?.activeExtension
                    ?.takeIf {
                        temporaryKind ==
                            TemporaryAccessKind.EXTEND
                    },
            onDismiss = {
                showExtendSheet =
                    false
            },
            onConfirm = { minutes ->

                viewModel
                    .extendDeviceAccess(
                        device.pdid,
                        minutes
                    )

                showExtendSheet =
                    false
            },
            onCancelExtension =
                if (
                    temporaryKind ==
                    TemporaryAccessKind.EXTEND
                ) {
                    {
                        viewModel
                            .cancelDeviceExtension(
                                device.pdid
                            )

                        showExtendSheet =
                            false
                    }
                } else {
                    null
                }
        )
    }

    if (
        showPauseSheet &&
        !isInfrastructure
    ) {

        PauseSheet(
            targetLabel =
                device.displayName,
            onDismiss = {
                showPauseSheet =
                    false
            },
            onConfirm = { minutes ->

                viewModel
                    .pauseDeviceInternet(
                        device.pdid,
                        minutes
                    )

                showPauseSheet =
                    false
            }
        )
    }

    if (
        showRenameDialog
    ) {

        DeviceRenameDialog(
            currentName =
                device.displayName,
            onDismiss = {
                showRenameDialog =
                    false
            },
            onConfirm = { name ->

                viewModel.renameDevice(
                    device.pdid,
                    name
                )

                showRenameDialog =
                    false
            }
        )
    }

    if (
        showUserAssignmentSheet
    ) {

        UserAssignmentSheet(
            users =
                state.users,
            assignedUserId =
                device.userID,
            onDismiss = {
                showUserAssignmentSheet =
                    false
            },
            onSelectUser = { userId ->

                viewModel
                    .assignDeviceUser(
                        device.pdid,
                        userId
                    )

                showUserAssignmentSheet =
                    false
            },
            onCreateUser = { user ->

                viewModel
                    .createUser(
                        user
                    )
            }
        )
    }
}

@Composable
private fun DeviceActionRow(
    status: EffectiveStatus?,
    presentation:
        com.lias.remote.core.util.EffectiveAccessPresentation,
    temporaryKind: TemporaryAccessKind,
    isInfrastructure: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onExtend: () -> Unit,
    onCancelExtension: () -> Unit,
    onRename: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        when {

            isInfrastructure -> {

                HigButton(
                    text =
                        "Protected",
                    onClick = {},
                    enabled =
                        false,
                    style =
                        HigButtonStyle.Gray,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            temporaryKind ==
                TemporaryAccessKind.PAUSE -> {

                HigButton(
                    text =
                        "Resume",
                    onClick =
                        onResume,
                    style =
                        HigButtonStyle.Primary,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                if (
                    status?.extendAvailable ==
                    true
                ) {

                    HigButton(
                        text =
                            "Extend Access",
                        onClick =
                            onExtend,
                        style =
                            HigButtonStyle.Secondary,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }

            temporaryKind ==
                TemporaryAccessKind.EXTEND -> {

                HigButton(
                    text =
                        "Cancel Extension",
                    onClick =
                        onCancelExtension,
                    style =
                        HigButtonStyle.Gray,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            presentation.canExtend -> {

                HigButton(
                    text =
                        "Extend Access",
                    onClick =
                        onExtend,
                    style =
                        HigButtonStyle.Secondary,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            presentation.canPause -> {

                HigButton(
                    text =
                        "Pause",
                    onClick =
                        onPause,
                    style =
                        HigButtonStyle.Gray,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            else -> {

                HigButton(
                    text =
                        "Checking",
                    onClick = {},
                    enabled =
                        false,
                    style =
                        HigButtonStyle.Gray,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }
        }

        HigButton(
            text =
                "Rename",
            onClick =
                onRename,
            style =
                HigButtonStyle.Gray,
            modifier =
                Modifier.weight(
                    1f
                )
        )
    }
}

private fun detailStatusText(
    title: String,
    temporaryKind: TemporaryAccessKind,
    minutes: Int?,
    isInfrastructure: Boolean
): String {

    if (
        isInfrastructure
    ) {
        return "Immune"
    }

    return when (
        temporaryKind
    ) {

        TemporaryAccessKind.PAUSE ->
            if (
                minutes !=
                    null &&
                minutes > 0
            ) {
                "Paused · ${formatTemporaryDuration(minutes)}"
            } else {
                "Paused"
            }

        TemporaryAccessKind.EXTEND ->
            if (
                minutes !=
                    null &&
                minutes > 0
            ) {
                "Extended · ${formatTemporaryDuration(minutes)}"
            } else {
                "Extended Access"
            }

        TemporaryAccessKind.NONE ->
            title
    }
}
