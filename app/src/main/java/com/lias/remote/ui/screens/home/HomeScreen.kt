// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt
// Version: 11.0.0
//
// Purpose:
//   LIAS network overview.
//
// Batch 11:
//   - Dashboard access metrics derive from EffectiveStatus.
//   - Pause quick action only chooses pause_available device.
//   - Extend quick action only chooses extend_available device.
//   - Temporary override count/status is server-derived.
//   - No pol_pause_* lookup.
//   - No fabricated countdowns.
// ====================================================================

package com.lias.remote.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.TemporaryAccessKind
import com.lias.remote.core.models.temporaryAccessKind
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
import com.lias.remote.ui.navigation.LiasTab
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.GlobalSwitchSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Airplane
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Pause
import io.github.alexzhirkevich.cupertino.icons.outlined.Shield

@Composable
fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail:
        (String) -> Unit,
    onNavigateToTab:
        (LiasTab) -> Unit
) {

    val state by
        viewModel.state
            .collectAsState()

    val scrollState =
        rememberLazyListState()

    var showGlobalSheet by
        remember {
            mutableStateOf(
                false
            )
        }

    var activeDeviceForExtend by
        remember {
            mutableStateOf<Device?>(
                null
            )
        }

    var activeDeviceForPause by
        remember {
            mutableStateOf<Device?>(
                null
            )
        }

    val globalPolicy =
        state.policies
            .find {
                it.id ==
                    "global_default"
            }
            ?: Policy(
                id =
                    "global_default",
                name =
                    "Global Access Switch",
                type =
                    "global",
                action =
                    "schedule"
            )

    val isVacationActive =
        globalPolicy.action ==
            "block"

    val totalDevices =
        state.devices.size

    val onlineDevices =
        state.devices.count {
            it.online
        }

    val offlineDevices =
        totalDevices -
            onlineDevices

    val knownStatuses =
        state.devices.mapNotNull { device ->
            state.deviceEffectiveStatuses[
                device.pdid
            ]
        }

    val blockedDevices =
        knownStatuses.count {
            it.action.equals(
                "block",
                true
            )
        }

    val allowedDevices =
        knownStatuses.count {
            it.action.equals(
                "allow",
                true
            )
        }

    val checkingDevices =
        (
            totalDevices -
                knownStatuses.size
            )
            .coerceAtLeast(
                0
            )

    val pausedDevices =
        knownStatuses.count {
            it.temporaryAccessKind ==
                TemporaryAccessKind.PAUSE
        }

    val extendedDevices =
        knownStatuses.count {
            it.temporaryAccessKind ==
                TemporaryAccessKind.EXTEND
        }

    val pauseCandidate =
        state.devices
            .firstOrNull { device ->

                state
                    .deviceEffectiveStatuses[
                        device.pdid
                    ]
                    ?.pauseAvailable ==
                    true
            }

    val extendCandidate =
        state.devices
            .firstOrNull { device ->

                state
                    .deviceEffectiveStatuses[
                        device.pdid
                    ]
                    ?.extendAvailable ==
                    true
            }

    HigLargeTitleScaffold(
        title =
            "Home",
        scrollState =
            scrollState,
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

            when (
                val sync =
                    state.syncState
            ) {

                SyncState.Idle,
                SyncState.Loading -> {

                    if (
                        !state.isInitialLoaded
                    ) {

                        item {

                            ScreenStateView(
                                title =
                                    "Loading LIAS",
                                message =
                                    "Synchronizing devices, rules and effective access state."
                            )
                        }

                        return@LazyColumn
                    }
                }

                is SyncState.Failed -> {

                    item {

                        ScreenStateView(
                            title =
                                "Unable to Load LIAS",
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

                    return@LazyColumn
                }

                is SyncState.Stale -> {

                    item {

                        StaleDataNotice(
                            message =
                                sync.message,
                            onRefresh =
                                viewModel::refresh
                        )
                    }
                }

                is SyncState.Ready ->
                    Unit
            }

            item {

                Column(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                16.dp,
                            vertical =
                                8.dp
                        )
                ) {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        16.dp
                                    )
                                )
                                .background(
                                    if (
                                        isVacationActive
                                    ) {
                                        LiasThemeColors.orange
                                            .copy(
                                                alpha = 0.14f
                                            )
                                    } else {
                                        LiasThemeColors.green
                                            .copy(
                                                alpha = 0.11f
                                            )
                                    }
                                )
                                .border(
                                    0.5.dp,
                                    if (
                                        isVacationActive
                                    ) {
                                        LiasThemeColors.orange
                                            .copy(
                                                alpha = 0.35f
                                            )
                                    } else {
                                        LiasThemeColors.green
                                            .copy(
                                                alpha = 0.28f
                                            )
                                    },
                                    RoundedCornerShape(
                                        16.dp
                                    )
                                )
                                .padding(
                                    20.dp
                                )
                    ) {

                        Column {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                CupertinoIcon(
                                    imageVector =
                                        if (
                                            isVacationActive
                                        ) {
                                            CupertinoIcons
                                                .Outlined
                                                .Airplane
                                        } else {
                                            CupertinoIcons
                                                .Outlined
                                                .Shield
                                        },
                                    contentDescription =
                                        null,
                                    tint =
                                        if (
                                            isVacationActive
                                        ) {
                                            LiasThemeColors.orange
                                        } else {
                                            LiasThemeColors.green
                                        },
                                    modifier =
                                        Modifier.size(
                                            17.dp
                                        )
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            6.dp
                                        )
                                )

                                CupertinoText(
                                    text =
                                        if (
                                            isVacationActive
                                        ) {
                                            "VACATION MODE ACTIVE"
                                        } else {
                                            "NETWORK STATUS"
                                        },
                                    style =
                                        HigTypography.caption,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color =
                                        LiasThemeColors
                                            .secondaryLabel
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            CupertinoText(
                                text =
                                    when {

                                        totalDevices ==
                                            0 ->
                                            "No devices discovered"

                                        onlineDevices ==
                                            1 ->
                                            "1 device online"

                                        else ->
                                            "$onlineDevices devices online"
                                    },
                                style =
                                    HigTypography.title1,
                                fontWeight =
                                    FontWeight.Bold,
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
                                    buildString {
                                        append(
                                            "$blockedDevices blocked"
                                        )

                                        if (
                                            pausedDevices >
                                            0
                                        ) {
                                            append(
                                                " · $pausedDevices paused"
                                            )
                                        }

                                        if (
                                            extendedDevices >
                                            0
                                        ) {
                                            append(
                                                " · $extendedDevices extended"
                                            )
                                        }
                                    },
                                style =
                                    HigTypography.subheadline,
                                color =
                                    LiasThemeColors
                                        .secondaryLabel
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        16.dp
                                    )
                            )

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        8.dp
                                    )
                            ) {

                                HigButton(
                                    text =
                                        "Global Switch",
                                    onClick = {
                                        showGlobalSheet =
                                            true
                                    },
                                    style =
                                        HigButtonStyle.Secondary,
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                )

                                HigButton(
                                    text =
                                        if (
                                            isVacationActive
                                        ) {
                                            "Vacation ON"
                                        } else {
                                            "Vacation"
                                        },
                                    onClick = {

                                        viewModel
                                            .toggleVacationMode(
                                                !isVacationActive
                                            )
                                    },
                                    style =
                                        if (
                                            isVacationActive
                                        ) {
                                            HigButtonStyle.Danger
                                        } else {
                                            HigButtonStyle.Secondary
                                        },
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                )
                            }
                        }
                    }
                }
            }

            if (
                totalDevices ==
                0
            ) {

                item {

                    ScreenStateView(
                        title =
                            "No Devices Yet",
                        message =
                            "LIAS is connected, but Discovery Service has not reported any devices yet.",
                        actionText =
                            "Refresh",
                        onAction =
                            viewModel::refresh
                    )
                }

            } else {

                item {

                    ListSectionHeader(
                        "Quick Actions"
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        16.dp
                                ),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {

                        QuickTile(
                            icon =
                                CupertinoIcons
                                    .Outlined
                                    .Iphone,
                            label =
                                "Devices",
                            color =
                                LiasThemeColors.blue,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {

                            onNavigateToTab(
                                LiasTab.DEVICES
                            )
                        }

                        QuickTile(
                            icon =
                                CupertinoIcons
                                    .Outlined
                                    .Clock,
                            label =
                                "Extend",
                            color =
                                LiasThemeColors.green,
                            enabled =
                                extendCandidate !=
                                    null,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {

                            activeDeviceForExtend =
                                extendCandidate
                        }

                        QuickTile(
                            icon =
                                CupertinoIcons
                                    .Outlined
                                    .Pause,
                            label =
                                "Pause",
                            color =
                                LiasThemeColors.orange,
                            enabled =
                                pauseCandidate !=
                                    null,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {

                            activeDeviceForPause =
                                pauseCandidate
                        }

                        QuickTile(
                            icon =
                                CupertinoIcons
                                    .Outlined
                                    .Clock,
                            label =
                                "Schedules",
                            color =
                                LiasThemeColors.indigo,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {

                            onNavigateToTab(
                                LiasTab.SCHEDULES
                            )
                        }
                    }
                }

                item {

                    ListSectionHeader(
                        "Effective Access"
                    )

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {

                        GroupedListRow(
                            primaryText =
                                "Allowed",
                            secondaryText =
                                "$allowedDevices devices",
                            trailingContent = {

                                StatusPill(
                                    text =
                                        allowedDevices
                                            .toString(),
                                    tone =
                                        PillTone.ALLOWED
                                )
                            },
                            showDivider =
                                true
                        )

                        GroupedListRow(
                            primaryText =
                                "Blocked",
                            secondaryText =
                                "$blockedDevices devices",
                            trailingContent = {

                                StatusPill(
                                    text =
                                        blockedDevices
                                            .toString(),
                                    tone =
                                        PillTone.BLOCKED
                                )
                            },
                            showDivider =
                                checkingDevices >
                                    0
                        )

                        if (
                            checkingDevices >
                            0
                        ) {

                            GroupedListRow(
                                primaryText =
                                    "Checking",
                                secondaryText =
                                    "Waiting for authoritative status",
                                trailingContent = {

                                    StatusPill(
                                        text =
                                            checkingDevices
                                                .toString(),
                                        tone =
                                            PillTone.INFO
                                    )
                                }
                            )
                        }
                    }
                }

                if (
                    pausedDevices >
                        0 ||
                    extendedDevices >
                        0
                ) {

                    item {

                        ListSectionHeader(
                            "Temporary Overrides"
                        )

                        GroupedListCard(
                            modifier =
                                Modifier.padding(
                                    horizontal =
                                        16.dp
                                )
                        ) {

                            if (
                                pausedDevices >
                                0
                            ) {

                                GroupedListRow(
                                    primaryText =
                                        "Paused",
                                    secondaryText =
                                        "$pausedDevices active",
                                    trailingContent = {

                                        StatusPill(
                                            text =
                                                pausedDevices
                                                    .toString(),
                                            tone =
                                                PillTone.PAUSED
                                        )
                                    },
                                    showDivider =
                                        extendedDevices >
                                            0
                                )
                            }

                            if (
                                extendedDevices >
                                0
                            ) {

                                GroupedListRow(
                                    primaryText =
                                        "Extended Access",
                                    secondaryText =
                                        "$extendedDevices active",
                                    trailingContent = {

                                        StatusPill(
                                            text =
                                                extendedDevices
                                                    .toString(),
                                            tone =
                                                PillTone.ALLOWED
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                item {

                    ListSectionHeader(
                        "Network Snapshot"
                    )

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical =
                                            14.dp
                                    ),
                            horizontalArrangement =
                                Arrangement.SpaceEvenly
                        ) {

                            MetricColumn(
                                value =
                                    totalDevices.toString(),
                                label =
                                    "Total",
                                color =
                                    LiasThemeColors.label
                            )

                            MetricColumn(
                                value =
                                    onlineDevices.toString(),
                                label =
                                    "Online",
                                color =
                                    LiasThemeColors.green
                            )

                            MetricColumn(
                                value =
                                    offlineDevices.toString(),
                                label =
                                    "Offline",
                                color =
                                    LiasThemeColors
                                        .tertiaryLabel
                            )
                        }
                    }
                }
            }
        }
    }

    if (
        showGlobalSheet
    ) {

        GlobalSwitchSheet(
            currentPolicy =
                globalPolicy,
            onDismiss = {
                showGlobalSheet =
                    false
            },
            onSave = { policy ->

                viewModel.savePolicy(
                    policy
                )

                showGlobalSheet =
                    false
            }
        )
    }

    activeDeviceForExtend
        ?.let { device ->

            val status =
                viewModel
                    .effectiveStatusFor(
                        device.pdid
                    )

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
                            status.temporaryAccessKind ==
                                TemporaryAccessKind.EXTEND
                        },
                onDismiss = {
                    activeDeviceForExtend =
                        null
                },
                onConfirm = { minutes ->

                    viewModel
                        .extendDeviceAccess(
                            device.pdid,
                            minutes
                        )

                    activeDeviceForExtend =
                        null
                }
            )
        }

    activeDeviceForPause
        ?.let { device ->

            PauseSheet(
                targetLabel =
                    device.displayName,
                onDismiss = {
                    activeDeviceForPause =
                        null
                },
                onConfirm = { minutes ->

                    viewModel
                        .pauseDeviceInternet(
                            device.pdid,
                            minutes
                        )

                    activeDeviceForPause =
                        null
                }
            )
        }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier =
        Modifier,
    enabled: Boolean =
        true,
    onClick: () -> Unit
) {

    Column(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    LiasThemeColors
                        .secondaryBackground
                )
                .border(
                    0.5.dp,
                    LiasThemeColors.separator,
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .clickable(
                    enabled =
                        enabled
                ) {
                    onClick()
                }
                .padding(
                    vertical =
                        12.dp,
                    horizontal =
                        6.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        36.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            9.dp
                        )
                    )
                    .background(
                        if (
                            enabled
                        ) {
                            color
                        } else {
                            LiasThemeColors.fill2
                        }
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            CupertinoIcon(
                imageVector =
                    icon,
                contentDescription =
                    label,
                tint =
                    if (
                        enabled
                    ) {
                        Color.White
                    } else {
                        LiasThemeColors
                            .tertiaryLabel
                    },
                modifier =
                    Modifier.size(
                        20.dp
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        CupertinoText(
            text =
                label,
            style =
                HigTypography.caption,
            color =
                if (
                    enabled
                ) {
                    LiasThemeColors.label
                } else {
                    LiasThemeColors
                        .tertiaryLabel
                },
            textAlign =
                TextAlign.Center
        )
    }
}

@Composable
private fun MetricColumn(
    value: String,
    label: String,
    color: Color
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        CupertinoText(
            text =
                value,
            style =
                HigTypography.title1,
            fontWeight =
                FontWeight.ExtraBold,
            color =
                color
        )

        CupertinoText(
            text =
                label.uppercase(),
            style =
                HigTypography.caption,
            color =
                color,
            fontWeight =
                FontWeight.Bold
        )
    }
}
