// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt
// Version: 6.0.0
//
// Purpose:
//   LIAS overview dashboard.
//
// UX corrections:
//   - No fabricated enforcement examples.
//   - No fabricated countdowns.
//   - Loading, failed, empty and stale states are distinct.
//   - All visible metrics derive from actual UiState.
//   - Quick actions requiring a device are disabled naturally when
//     there is no eligible device.
//   - Uses the typed LiasTab navigation contract from Batch 3.
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
import com.lias.remote.repositories.SyncState
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
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
    onNavigateToDeviceDetail: (String) -> Unit,
    onNavigateToTab: (LiasTab) -> Unit
) {
    val state by
        viewModel.state.collectAsState()

    val scrollState =
        rememberLazyListState()

    var showGlobalSheet by
        remember {
            mutableStateOf(false)
        }

    var activeDeviceForExtend by
        remember {
            mutableStateOf<Device?>(null)
        }

    var activeDeviceForPause by
        remember {
            mutableStateOf<Device?>(null)
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

    val blockedDevices =
        state.devices.count { device ->
            state
                .deviceEffectiveStatuses[
                    device.pdid
                ]
                ?.action ==
                "block"
        }

    val activePolicies =
        state.policies.filter {
            it.enabled
        }

    val eligibleDevices =
        state.devices.filterNot {
            it.safeTags.contains(
                "infrastructure"
            )
        }

    HigLargeTitleScaffold(
        title = "Home",
        scrollState =
            scrollState,
        navTrailing = {
            HigTextButton(
                text = "Refresh",
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
                                    "Synchronizing devices, rules and schedules."
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
                            horizontal = 16.dp,
                            vertical = 8.dp
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
                                    width =
                                        0.5.dp,
                                    color =
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
                                    shape =
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
                                        if (
                                            isVacationActive
                                        ) {
                                            LiasThemeColors.orange
                                        } else {
                                            LiasThemeColors.secondaryLabel
                                        }
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        7.dp
                                    )
                            )

                            CupertinoText(
                                text =
                                    when {
                                        totalDevices == 0 ->
                                            "No devices discovered"

                                        onlineDevices == 1 ->
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
                                    "${activePolicies.size} enabled rules · $blockedDevices blocked now",
                                style =
                                    HigTypography.subheadline,
                                color =
                                    LiasThemeColors.secondaryLabel
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
                totalDevices == 0
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
                                eligibleDevices.isNotEmpty(),
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {
                            activeDeviceForExtend =
                                eligibleDevices
                                    .firstOrNull()
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
                                eligibleDevices.isNotEmpty(),
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {
                            activeDeviceForPause =
                                eligibleDevices
                                    .firstOrNull()
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
                        text =
                            "Access Now",
                        trailingAction = {
                            HigTextButton(
                                text =
                                    "View Devices",
                                onClick = {
                                    onNavigateToTab(
                                        LiasTab.DEVICES
                                    )
                                }
                            )
                        }
                    )

                    GroupedListCard(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    16.dp
                            )
                    ) {
                        AccessSummaryRow(
                            title =
                                "Allowed",
                            value =
                                totalDevices -
                                    blockedDevices,
                            tone =
                                PillTone.ALLOWED
                        )

                        AccessSummaryRow(
                            title =
                                "Blocked",
                            value =
                                blockedDevices,
                            tone =
                                PillTone.BLOCKED
                        )
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
                                Arrangement.SpaceEvenly,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            MetricColumn(
                                value =
                                    "$totalDevices",
                                label =
                                    "Total",
                                color =
                                    LiasThemeColors.label
                            )

                            DividerMetric()

                            MetricColumn(
                                value =
                                    "$onlineDevices",
                                label =
                                    "Online",
                                color =
                                    LiasThemeColors.green
                            )

                            DividerMetric()

                            MetricColumn(
                                value =
                                    "$offlineDevices",
                                label =
                                    "Offline",
                                color =
                                    LiasThemeColors.tertiaryLabel
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
                viewModel.effectiveStatusFor(
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
                    status?.activeExtension,
                onDismiss = {
                    activeDeviceForExtend =
                        null
                },
                onConfirm = { minutes ->
                    viewModel.extendDeviceAccess(
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
                    viewModel.pauseDeviceInternet(
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val foreground =
        if (enabled) {
            LiasThemeColors.label
        } else {
            LiasThemeColors.tertiaryLabel
        }

    Column(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .background(
                    LiasThemeColors.secondaryBackground
                )
                .border(
                    width =
                        0.5.dp,
                    color =
                        LiasThemeColors.separator,
                    shape =
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
                        LiasThemeColors.tertiaryLabel
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
                foreground,
            textAlign =
                TextAlign.Center,
            maxLines =
                1
        )
    }
}

@Composable
private fun AccessSummaryRow(
    title: String,
    value: Int,
    tone: PillTone
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        CupertinoText(
            text =
                title,
            style =
                HigTypography.body,
            color =
                LiasThemeColors.label
        )

        StatusPill(
            text =
                value.toString(),
            tone =
                tone
        )
    }
}

@Composable
private fun DividerMetric() {
    Box(
        modifier =
            Modifier
                .width(
                    0.5.dp
                )
                .height(
                    30.dp
                )
                .background(
                    LiasThemeColors.separator
                )
    )
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
