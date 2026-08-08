// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt
// Version: 7.0.0
//
// Purpose:
//   Detailed device inspection and control.
//
// Corrections:
//   - Never returns a blank screen for an unresolved/stale PDID.
//   - Handles initial loading separately from not-found.
//   - EffectiveStatus is nullable until authoritative state arrives.
//   - Infrastructure devices never expose pause/extend controls.
//   - Removes emoji device artwork.
//   - Uses actual identity-tier/confidence information.
//   - Uses functional DeviceRenameDialog.
//   - Uses implemented UserAssignmentSheet.
//   - LIAS generates new User IDs.
//   - Activity log errors are represented explicitly.
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.DeviceIdentityFormatter
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.network.ApiResult
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
        viewModel.state.collectAsState()

    val scrollState =
        rememberLazyListState()

    val device =
        state.devices.find {
            it.pdid == pdid
        }

    /*
     * A deep link can arrive before inventory synchronization finishes.
     *
     * Do not interpret "not in current list yet" as a permanent
     * not-found condition while the initial synchronization is active.
     */
    if (device == null) {

        HigLargeTitleScaffold(
            title = "Device",
            scrollState =
                scrollState,
            navLeading = {
                HigTextButton(
                    text = "‹ Devices",
                    onClick = onBack
                )
            }
        ) { padding ->

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment =
                    Alignment.Center
            ) {

                when {
                    state.syncState is
                        SyncState.Loading ||
                        state.syncState is
                        SyncState.Idle -> {

                        ScreenStateView(
                            title =
                                "Loading Device",
                            message =
                                "Waiting for the LIAS device inventory."
                        )
                    }

                    state.syncState is
                        SyncState.Failed -> {

                        val failure =
                            state.syncState
                                as SyncState.Failed

                        ScreenStateView(
                            title =
                                "Unable to Load Device",
                            message =
                                failure.message,
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
        viewModel.state.collectAsState()

    val scrollState =
        rememberLazyListState()

    var logs by
        remember(device.pdid) {
            mutableStateOf<
                List<FlowLog>
            >(emptyList())
        }

    var isLoadingLogs by
        remember(device.pdid) {
            mutableStateOf(true)
        }

    var logError by
        remember(device.pdid) {
            mutableStateOf<String?>(null)
        }

    var showExtendSheet by
        remember {
            mutableStateOf(false)
        }

    var showPauseSheet by
        remember {
            mutableStateOf(false)
        }

    var showUserAssignmentSheet by
        remember {
            mutableStateOf(false)
        }

    var showRenameDialog by
        remember {
            mutableStateOf(false)
        }

    val isPaused =
        state.policies.any {
            it.id ==
                "pol_pause_${device.pdid}" &&
                it.enabled
        }

    val assignedUser =
        state.users.find {
            it.id ==
                device.userID
        }

    val isInfrastructure =
        device.safeTags.contains(
            "infrastructure"
        )

    val effectiveStatus =
        viewModel.effectiveStatusFor(
            device.pdid
        )

    val isBlocked =
        effectiveStatus
            ?.action
            ?.equals(
                "block",
                ignoreCase = true
            ) == true

    val presentation =
        remember(device) {
            DeviceIdentityFormatter.present(
                device
            )
        }

    LaunchedEffect(
        device.pdid
    ) {
        isLoadingLogs = true
        logError = null

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
                    result.cause.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to load activity."
            }

            is ApiResult.SerializationError -> {
                logError =
                    "The server returned invalid activity data."
            }
        }

        isLoadingLogs = false
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

            // --------------------------------------------------------
            // Device hero
            // --------------------------------------------------------

            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
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
                                .clip(
                                    RoundedCornerShape(
                                        17.dp
                                    )
                                )
                                .background(
                                    LiasThemeColors.blue
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
                                androidx.compose.ui.graphics.Color.White,
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
                            presentation.subtitle,
                        style =
                            HigTypography.body,
                        color =
                            if (device.online) {
                                LiasThemeColors.green
                            } else {
                                LiasThemeColors.tertiaryLabel
                            },
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )

                    StatusPill(
                        text =
                            accessStatusText(
                                isInfrastructure =
                                    isInfrastructure,
                                isPaused =
                                    isPaused,
                                effectiveStatus =
                                    effectiveStatus
                            ),
                        tone =
                            accessStatusTone(
                                isInfrastructure =
                                    isInfrastructure,
                                isPaused =
                                    isPaused,
                                effectiveStatus =
                                    effectiveStatus
                            )
                    )

                    if (isInfrastructure) {
                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )

                        CupertinoText(
                            text =
                                "Infrastructure devices are protected from pause and schedule overrides.",
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors.secondaryLabel
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                16.dp
                            )
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        if (!isInfrastructure) {

                            when {

                                isPaused -> {
                                    HigButton(
                                        text =
                                            "Resume",
                                        onClick = {
                                            viewModel
                                                .unpauseDeviceInternet(
                                                    device.pdid
                                                )
                                        },
                                        style =
                                            HigButtonStyle.Primary,
                                        modifier =
                                            Modifier.weight(
                                                1f
                                            )
                                    )
                                }

                                isBlocked -> {
                                    HigButton(
                                        text =
                                            "Extend Access",
                                        onClick = {
                                            showExtendSheet =
                                                true
                                        },
                                        style =
                                            HigButtonStyle.Secondary,
                                        enabled =
                                            effectiveStatus !=
                                                null,
                                        modifier =
                                            Modifier.weight(
                                                1f
                                            )
                                    )
                                }

                                else -> {
                                    HigButton(
                                        text =
                                            "Pause",
                                        onClick = {
                                            showPauseSheet =
                                                true
                                        },
                                        style =
                                            HigButtonStyle.Gray,
                                        enabled =
                                            effectiveStatus !=
                                                null,
                                        modifier =
                                            Modifier.weight(
                                                1f
                                            )
                                    )
                                }
                            }
                        }

                        HigButton(
                            text =
                                "Rename",
                            onClick = {
                                showRenameDialog =
                                    true
                            },
                            style =
                                HigButtonStyle.Gray,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }
                }
            }

            // --------------------------------------------------------
            // Identity
            // --------------------------------------------------------

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
                                    "${presentation.confidencePercent}%",
                                style =
                                    HigTypography.subheadline,
                                color =
                                    LiasThemeColors.secondaryLabel
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
                            "Device Type",
                        secondaryText =
                            device.deviceType
                                .ifBlank {
                                    "Unclassified"
                                },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Assigned User",
                        secondaryText =
                            assignedUser
                                ?.name
                                ?: "Unassigned",
                        trailingContent = {
                            CupertinoText(
                                text =
                                    "›",
                                style =
                                    HigTypography.headline,
                                color =
                                    LiasThemeColors.tertiaryLabel
                            )
                        },
                        onClick = {
                            showUserAssignmentSheet =
                                true
                        }
                    )
                }
            }

            // --------------------------------------------------------
            // Discovery detail
            // --------------------------------------------------------

            if (
                device.safeServices.isNotEmpty() ||
                device.safeIps.size > 1 ||
                device.safeMacs.size > 1
            ) {
                item {
                    ListSectionHeader(
                        "Discovery"
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

                        if (
                            device.safeIps.size >
                            1
                        ) {
                            GroupedListRow(
                                primaryText =
                                    "Known IP Addresses",
                                secondaryText =
                                    device.safeIps
                                        .joinToString(
                                            ", "
                                        ),
                                showDivider =
                                    device.safeMacs.size > 1 ||
                                        device.safeServices.isNotEmpty()
                            )
                        }

                        if (
                            device.safeMacs.size >
                            1
                        ) {
                            GroupedListRow(
                                primaryText =
                                    "Known MAC Addresses",
                                secondaryText =
                                    device.safeMacs
                                        .joinToString(
                                            ", "
                                        ),
                                showDivider =
                                    device.safeServices.isNotEmpty()
                            )
                        }

                        if (
                            device.safeServices
                                .isNotEmpty()
                        ) {
                            GroupedListRow(
                                primaryText =
                                    "Observed Services",
                                secondaryText =
                                    device.safeServices
                                        .joinToString(
                                            ", "
                                        )
                            )
                        }
                    }
                }
            }

            // --------------------------------------------------------
            // Activity
            // --------------------------------------------------------

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

                        logError != null -> {
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
                                    "LIAS has no recent activity entries for this device."
                            )
                        }

                        else -> {
                            logs.forEachIndexed { index, log ->

                                val action =
                                    log.action
                                        .lowercase()

                                GroupedListRow(
                                    primaryText =
                                        log.timestamp
                                            .ifBlank {
                                                "Activity"
                                            },
                                    secondaryText =
                                        if (
                                            log.bytes > 0
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
                                                when (action) {
                                                    "block" ->
                                                        PillTone.BLOCKED

                                                    "allow" ->
                                                        PillTone.ALLOWED

                                                    else ->
                                                        PillTone.INFO
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

    // ------------------------------------------------------------
    // Sheets / dialogs
    // ------------------------------------------------------------

    if (showExtendSheet) {

        val cancelAction:
            (() -> Unit)? =
            if (
                effectiveStatus
                    ?.activeExtension !=
                null
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

        ExtendAccessSheet(
            targetLabel =
                device.displayName,
            targetSubtitle =
                device.currentIP
                    .ifBlank {
                        device.pdid
                    },
            currentExtension =
                effectiveStatus
                    ?.activeExtension,
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
                cancelAction
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

    if (showRenameDialog) {
        DeviceRenameDialog(
            currentName =
                device.displayName,
            onDismiss = {
                showRenameDialog =
                    false
            },
            onConfirm = { newName ->
                viewModel.renameDevice(
                    device.pdid,
                    newName
                )

                showRenameDialog =
                    false
            }
        )
    }

    if (showUserAssignmentSheet) {
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
                viewModel.assignDeviceUser(
                    device.pdid,
                    userId
                )

                showUserAssignmentSheet =
                    false
            },
            onCreateUser = { user ->
                viewModel.createUser(
                    user
                )
            }
        )
    }
}

private fun accessStatusText(
    isInfrastructure: Boolean,
    isPaused: Boolean,
    effectiveStatus: EffectiveStatus?
): String =
    when {

        isInfrastructure ->
            "Immune"

        isPaused ->
            "Paused"

        effectiveStatus == null ->
            "Checking"

        effectiveStatus.action.equals(
            "block",
            ignoreCase = true
        ) ->
            "Blocked"

        effectiveStatus.action.equals(
            "allow",
            ignoreCase = true
        ) ->
            "Allowed"

        else ->
            "Unknown"
    }

private fun accessStatusTone(
    isInfrastructure: Boolean,
    isPaused: Boolean,
    effectiveStatus: EffectiveStatus?
): PillTone =
    when {

        isInfrastructure ->
            PillTone.INFO

        isPaused ->
            PillTone.PAUSED

        effectiveStatus == null ->
            PillTone.INFO

        effectiveStatus.action.equals(
            "block",
            ignoreCase = true
        ) ->
            PillTone.BLOCKED

        effectiveStatus.action.equals(
            "allow",
            ignoreCase = true
        ) ->
            PillTone.ALLOWED

        else ->
            PillTone.INFO
    }
