// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt
// Version: 27.2.0
//
// Purpose:
//   Authoritative device detail and actions.
//
// Batch 26:
//   - Explicit unavailable-device state.
//   - EffectiveStatus controls every Internet action.
//   - No pol_pause_<pdid> inspection.
//   - Pause uses dedicated fixed-one-hour endpoint.
//   - Extend supports active extension management/cancellation.
//   - New User is sent with id="" for LIAS/server ownership.
//   - Removes emoji device/action icons.
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.access.AccessPresentationResolver
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusDot
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
        viewModel.state
            .collectAsState()

    val scrollState =
        rememberLazyListState()

    val device =
        state.devices
            .find {
                it.pdid ==
                    pdid
            }

    /*
     * Do not show missing-state during initial inventory hydration.
     */
    if (
        device ==
        null
    ) {

        DeviceUnavailableScreen(
            isLoading =
                !state.isInitialLoaded,
            onBack =
                onBack,
            onRefresh =
                viewModel::refresh
        )

        return
    }

    val status =
        state
            .effectiveStatusForDevice(
                device.pdid
            )

    val access =
        AccessPresentationResolver
            .resolve(
                device =
                    device,
                status =
                    status
            )

    val assignedUser =
        state.users
            .find {
                it.id ==
                    device.userID
            }

    var logs by
        remember(
            pdid
        ) {
            mutableStateOf<
                List<FlowLog>
            >(
                emptyList()
            )
        }

    var isLoadingLogs by
        remember(
            pdid
        ) {
            mutableStateOf(
                true
            )
        }

    var logsError by
        remember(
            pdid
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

    var showTagAssignmentSheet by
        remember {
            mutableStateOf(
                false
            )
        }

    LaunchedEffect(
        pdid
    ) {

        isLoadingLogs =
            true

        logsError =
            null

        when (
            val result =
                viewModel
                    .getDeviceLogs(
                        pdid
                    )
        ) {

            is ApiResult.Success ->

                logs =
                    result.data

            else ->

                logsError =
                    "Recent activity could not be loaded."
        }

        isLoadingLogs =
            false
    }

    HigLargeTitleScaffold(
        title =
            "",
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
    ) {
        padding ->

        LazyColumn(
            state =
                scrollState,
            modifier =
                Modifier.fillMaxSize(),
            contentPadding =
                padding
        ) {

            item(
                key =
                    "profile"
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                16.dp
                            ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    64.dp
                                )
                                .background(
                                    color =
                                        LiasThemeColors.blue,
                                    shape =
                                        RoundedCornerShape(
                                            16.dp
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
                                androidx.compose.ui.graphics.Color.White,
                            modifier =
                                Modifier.size(
                                    34.dp
                                )
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )

                    CupertinoText(
                        text =
                            device.displayName,
                        style =
                            HigTypography.title1,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            LiasThemeColors.label,
                        textAlign =
                            TextAlign.Center
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        StatusDot(
                            isOnline =
                                device.online,
                            isPaused =
                                access.isPaused
                        )

                        CupertinoText(
                            text =
                                if (
                                    device.online
                                ) {
                                    device.currentIP
                                        .takeIf {
                                            it.isNotBlank()
                                        }
                                        ?.let {
                                            "Online · $it"
                                        }
                                        ?: "Online"
                                } else {
                                    "Offline"
                                },
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

                        StatusPill(
                            text =
                                access.label,
                            tone =
                                access.tone
                        )
                    }

                    if (
                        !access.isInfrastructure
                    ) {

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

                            when {

                                access.canResumePause ->

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

                                access.canManageExtension ->

                                    HigButton(
                                        text =
                                            "Manage Access",
                                        onClick = {
                                            showExtendSheet =
                                                true
                                        },
                                        style =
                                            HigButtonStyle.Secondary,
                                        modifier =
                                            Modifier.weight(
                                                1f
                                            )
                                    )

                                access.canExtend ->

                                    HigButton(
                                        text =
                                            "Extend Access",
                                        onClick = {
                                            showExtendSheet =
                                                true
                                        },
                                        style =
                                            HigButtonStyle.Secondary,
                                        modifier =
                                            Modifier.weight(
                                                1f
                                            )
                                    )

                                access.canPause ->

                                    HigButton(
                                        text =
                                            "Pause",
                                        onClick = {
                                            showPauseSheet =
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

                    } else {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )

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
                                Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                ListSectionHeader(
                    "Access"
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
                            "Effective Access",
                        secondaryText =
                            access.label,
                        trailingContent = {

                            StatusPill(
                                text =
                                    access.label,
                                tone =
                                    access.tone
                            )
                        },
                        showDivider =
                            true
                    )

                    GroupedListRow(
                        primaryText =
                            "Source",
                        secondaryText =
                            access.source
                                ?.replace(
                                    "_",
                                    " "
                                )
                                ?.replaceFirstChar {
                                    it.uppercase()
                                }
                                ?: "Not available",
                        showDivider =
                            status
                                ?.activeExtension !=
                                null
                    )

                    status
                        ?.activeExtension
                        ?.let {
                            extension ->

                            GroupedListRow(
                                primaryText =
                                    if (
                                        extension.reasonTag ==
                                        "pause"
                                    ) {
                                        "Pause Timer"
                                    } else {
                                        "Access Extension"
                                    },
                                secondaryText =
                                    "${extension.minutesLeft.coerceAtLeast(0)} minutes remaining"
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
                            "Hostname",
                        secondaryText =
                            device.hostname
                                .ifBlank {
                                    device.canonicalHostname
                                }
                                .ifBlank {
                                    "Not available"
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
                                    "Not available"
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
                            "Type",
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
                            "Identity Tier",
                        secondaryText =
                            device.identityTier
                                .replaceFirstChar {
                                    it.uppercase()
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
                                    LiasThemeColors
                                        .tertiaryLabel
                            )
                        },
                        onClick = {
                            showUserAssignmentSheet =
                                true
                        }
                    )
                }
            }

            item {
                ListSectionHeader(
                    "Tags"
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
                        device.safeTags
                            .isEmpty()
                    ) {

                        GroupedListRow(
                            primaryText =
                                "Generic",
                            secondaryText =
                                "No explicit tag assignment"
                        )

                    } else {

                        device.safeTags
                            .forEachIndexed {
                                    index,
                                    tagId ->

                                val tag =
                                    state.tags
                                        .find {
                                            it.id ==
                                                tagId
                                        }

                                GroupedListRow(
                                    primaryText =
                                        tag
                                            ?.name
                                            ?: tagId,
                                    secondaryText =
                                        if (
                                            tagId ==
                                            "infrastructure"
                                        ) {
                                            "Protected infrastructure"
                                        } else {
                                            "Policy membership"
                                        },
                                    showDivider =
                                        index <
                                            device.safeTags
                                                .lastIndex
                                )
                            }
                    }
                }
            }

            item {

                HigButton(
                    text =
                        "Manage Tags",
                    onClick = {
                        showTagAssignmentSheet =
                            true
                    },
                    style =
                        HigButtonStyle.Secondary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    16.dp
                            )
                )
            }

            item {
                ListSectionHeader(
                    "Activity · Last 24 Hours"
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

                        isLoadingLogs ->

                            GroupedListRow(
                                primaryText =
                                    "Loading activity…"
                            )

                        logsError !=
                            null ->

                            GroupedListRow(
                                primaryText =
                                    "Activity unavailable",
                                secondaryText =
                                    logsError
                            )

                        logs.isEmpty() ->

                            GroupedListRow(
                                primaryText =
                                    "No recent activity"
                            )

                        else ->

                            logs.forEachIndexed {
                                    index,
                                    log ->

                                val blocked =
                                    log.action.equals(
                                        "block",
                                        ignoreCase =
                                            true
                                    )

                                GroupedListRow(
                                    primaryText =
                                        log.timestamp,
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
                                                if (
                                                    blocked
                                                ) {
                                                    "Blocked"
                                                } else {
                                                    "Allowed"
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

    if (
        showExtendSheet &&
        (
            access.canExtend ||
            access.canManageExtension
            )
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
                    ?.activeExtension,
            onDismiss = {
                showExtendSheet =
                    false
            },
            onConfirm = {
                minutes ->

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
                    access.canManageExtension
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
        access.canPause
    ) {

        PauseSheet(
            targetLabel =
                device.displayName,
            onDismiss = {
                showPauseSheet =
                    false
            },
            onConfirm = {
                _ ->

                viewModel
                    .pauseDeviceInternet(
                        device.pdid
                    )

                showPauseSheet =
                    false
            }
        )
    }

    if (
        showTagAssignmentSheet
    ) {

        MoveTagSheet(
            device =
                device,
            allTags =
                state.tags,
            onDismiss = {
                showTagAssignmentSheet =
                    false
            },
            onConfirm = {
                tagIds ->

                viewModel
                    .assignTags(
                        device.pdid,
                        tagIds
                    )

                showTagAssignmentSheet =
                    false
            }
        )
    }

    if (
        showRenameDialog
    ) {

        RenameDeviceDialog(
            currentName =
                device.displayName,
            onDismiss = {
                showRenameDialog =
                    false
            },
            onConfirm = {
                newName ->

                viewModel
                    .renameDevice(
                        device.pdid,
                        newName
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
            onSelectUser = {
                userId ->

                viewModel
                    .assignDeviceUser(
                        device.pdid,
                        userId
                    )

                showUserAssignmentSheet =
                    false
            },
            onCreateUser = {
                user ->

                viewModel
                    .createUser(
                        user
                    )
            }
        )
    }
}

@Composable
private fun DeviceUnavailableScreen(
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    24.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        CupertinoText(
            text =
                if (
                    isLoading
                ) {
                    "Loading Device…"
                } else {
                    "Device Unavailable"
                },
            style =
                HigTypography.title2,
            fontWeight =
                FontWeight.Bold,
            color =
                LiasThemeColors.label,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        CupertinoText(
            text =
                if (
                    isLoading
                ) {
                    "Waiting for the current LIAS inventory."
                } else {
                    "This device may have been removed, reidentified, or is no longer returned by LIAS."
                },
            style =
                HigTypography.body,
            color =
                LiasThemeColors.secondaryLabel,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )

        if (
            !isLoading
        ) {

            HigButton(
                text =
                    "Refresh",
                onClick =
                    onRefresh,
                style =
                    HigButtonStyle.Secondary,
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )
        }

        HigButton(
            text =
                "Back to Devices",
            onClick =
                onBack,
            style =
                HigButtonStyle.Gray,
            modifier =
                Modifier.fillMaxWidth()
        )
    }
}
