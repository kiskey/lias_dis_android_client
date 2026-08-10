// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt
// Version: 26.0.0
//
// Purpose:
//   LIAS operational overview.
//
// Batch 26:
//   - Server-authoritative device actions.
//   - No pause-policy ID inspection.
//   - No fake status calculation.
//   - No emoji action labels.
//   - Quick navigation remains lightweight.
//   - Global policy remains server-owned global_default.
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.access.AccessKind
import com.lias.remote.ui.access.AccessPresentationResolver
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusDot
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.navigation.LiasScreen
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.GlobalSwitchSheet
import com.lias.remote.ui.screens.PauseSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Shield

@Composable
fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit,
    onNavigateToTab: (LiasScreen) -> Unit,
    onNavigateToIdentityReview: () -> Unit = {}
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
                    "Global Access",
                type =
                    "global",
                targetID =
                    "",
                action =
                    "allow",
                priority =
                    0,
                enabled =
                    true
            )

    val totalDevices =
        state.devices.size

    val onlineDevices =
        state.devices
            .count {
                it.online
            }

    val blockedDevices =
        state.devices
            .count {
                device ->

                state
                    .effectiveStatusForDevice(
                        device.pdid
                    )
                    ?.action
                    ?.equals(
                        "block",
                        ignoreCase =
                            true
                    ) ==
                    true
            }

    val attentionDevices =
        state.devices
            .filter {
                device ->

                val effectiveStatus =
                    state.effectiveStatusForDevice(
                        device.pdid
                    )

                !effectiveStatus
                    ?.source
                    .equals(
                        "global",
                        ignoreCase = true
                    ) &&
                when (
                    AccessPresentationResolver
                        .resolve(
                            device,
                            effectiveStatus
                        )
                        .kind
                ) {

                    AccessKind.PAUSED,
                    AccessKind.BLOCKED,
                    AccessKind.EXTENDED ->
                        true

                    else ->
                        false
                }
            }
            .sortedWith(
                compareBy<Device> {
                    device ->

                    when (
                        AccessPresentationResolver
                            .resolve(
                                device,
                                state
                                    .effectiveStatusForDevice(
                                        device.pdid
                                    )
                            )
                            .kind
                    ) {

                        AccessKind.PAUSED -> 0
                        AccessKind.BLOCKED -> 1
                        AccessKind.EXTENDED -> 2
                        else -> 3
                    }
                }.thenBy {
                    it.displayName.lowercase()
                }
            )

    val activeTagEnforcements =
        state.tags
            .mapNotNull { tag ->
                val status =
                    state.effectiveStatusForTag(tag.id)

                if (
                    status != null &&
                    !status.source.equals(
                        "global",
                        ignoreCase = true
                    ) &&
                    (
                        status.action.equals(
                            "block",
                            ignoreCase = true
                        ) ||
                        status.activeExtension != null
                    )
                ) {
                    tag to status
                } else {
                    null
                }
            }

    val hasGlobalEnforcement =
        state.isInitialLoaded &&
            state.policies.any {
                it.id == "global_default"
            } &&
            globalPolicy.enabled &&
            globalPolicy.action.lowercase() in
            setOf("allow", "block")
HigLargeTitleScaffold(
        title =
            "Home",
        scrollState =
            scrollState
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
                            "Global Access",
                        secondaryText =
                            when (
                                globalPolicy.action
                            ) {

                                "block" ->
                                    "Block All"

                                "schedule" ->
                                    "Schedule"

                                else ->
                                    "Allow All"
                            },
                        trailingContent = {

                            StatusPill(
                                text =
                                    when (
                                        globalPolicy.action
                                    ) {

                                        "block" ->
                                            "Block All"

                                        "schedule" ->
                                            "Scheduled"

                                        else ->
                                            "Allow All"
                                    },
                                tone =
                                    when (
                                        globalPolicy.action
                                    ) {

                                        "block" ->
                                            PillTone.BLOCKED

                                        "schedule" ->
                                            PillTone.SCHEDULED

                                        else ->
                                            PillTone.ALLOWED
                                    }
                            )
                        },
                        onClick = {
                            showGlobalSheet =
                                true
                        }
                    )
                }
            }

            if (state.capabilities != null) {
                item {
                    ListSectionHeader("LIAS 2.0")
                }

                item {
                    GroupedListCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        val upstream =
                            state.systemStatus?.upstream
                                ?: state.capabilities?.upstream

                        GroupedListRow(
                            primaryText = "LIAS Engine",
                            secondaryText =
                                buildString {
                                    append(
                                        "API ${state.capabilities?.apiVersion} · schema ${state.capabilities?.schemaVersion}"
                                    )
                                    state.snapshotRevision?.let {
                                        append(" · snapshot $it")
                                    }
                                },
                            trailingContent = {
                                StatusPill(
                                    text =
                                        when {
                                            upstream?.legacyMode == true -> "Legacy DIS"
                                            upstream?.reachable == false -> "Degraded"
                                            else -> "Current"
                                        },
                                    tone =
                                        when {
                                            upstream?.legacyMode == true -> PillTone.INFO
                                            upstream?.reachable == false -> PillTone.WARN
                                            else -> PillTone.ALLOWED
                                        }
                                )
                            },
                            showDivider = state.supportsIdentityReview
                        )

                        if (state.supportsIdentityReview) {
                            GroupedListRow(
                                primaryText = "Identity Review",
                                secondaryText =
                                    if (
                                        state.identityReview.pendingCount == 0
                                    ) {
                                        "No pending possible matches"
                                    } else {
                                        "Review evidence before merging device records"
                                    },
                                trailingContent = {
                                    StatusPill(
                                        text =
                                            buildString {
                                                append(state.identityReview.pendingCount)
                                                if (
                                                    state.identityReview.pendingHasMore
                                                ) {
                                                    append("+")
                                                }
                                            },
                                        tone =
                                            if (
                                                state.identityReview.pendingCount == 0
                                            ) {
                                                PillTone.INFO
                                            } else {
                                                PillTone.WARN
                                            }
                                    )
                                },
                                onClick = onNavigateToIdentityReview
                            )
                        }
                    }
                }
            }

            item {

                ListSectionHeader(
                    "Quick Access"
                )
            }

            item {

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
                            ),
                        onClick = {
                            onNavigateToTab(
                                LiasScreen.Devices
                            )
                        }
                    )

                    QuickTile(
                        icon =
                            CupertinoIcons
                                .Outlined
                                .Clock,
                        label =
                            "Schedules",
                        color =
                            LiasThemeColors.orange,
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        onClick = {
                            onNavigateToTab(
                                LiasScreen.Schedules
                            )
                        }
                    )

                    QuickTile(
                        icon =
                            CupertinoIcons
                                .Outlined
                                .Shield,
                        label =
                            "Rules",
                        color =
                            LiasThemeColors.green,
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        onClick = {
                            onNavigateToTab(
                                LiasScreen.Rules
                            )
                        }
                    )
                }
            }

            item {

                ListSectionHeader(
                    "Network Snapshot"
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

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical =
                                        16.dp
                                ),
                        horizontalArrangement =
                            Arrangement.SpaceEvenly
                    ) {

                        MetricColumn(
                            value =
                                totalDevices
                                    .toString(),
                            label =
                                "Total"
                        )

                        MetricColumn(
                            value =
                                onlineDevices
                                    .toString(),
                            label =
                                "Online"
                        )

                        MetricColumn(
                            value =
                                blockedDevices
                                    .toString(),
                            label =
                                "Blocked"
                        )
                    }
                }
            }

            if (
                state.errorMessage !=
                null
            ) {

                item {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        16.dp,
                                    vertical =
                                        8.dp
                                )
                                .background(
                                    color =
                                        LiasThemeColors.orange
                                            .copy(
                                                alpha =
                                                    0.10f
                                            ),
                                    shape =
                                        RoundedCornerShape(
                                            12.dp
                                        )
                                )
                                .padding(
                                    12.dp
                                )
                    ) {

                        CupertinoText(
                            text =
                                "Some LIAS data could not be refreshed.",
                            style =
                                HigTypography.headline,
                            color =
                                LiasThemeColors.label
                        )

                        CupertinoText(
                            text =
                                state.errorMessage
                                    .orEmpty(),
                            style =
                                HigTypography.subheadline,
                            color =
                                LiasThemeColors
                                    .secondaryLabel
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )

                        HigButton(
                            text =
                                "Retry",
                            onClick =
                                viewModel::refresh,
                            style =
                                HigButtonStyle.Secondary
                        )
                    }
                }
            }

            if (
                attentionDevices.isNotEmpty() ||
                activeTagEnforcements.isNotEmpty() ||
                hasGlobalEnforcement
            ) {

                item {

                    ListSectionHeader(
                        "Active Enforcements · ${attentionDevices.size + activeTagEnforcements.size + if (hasGlobalEnforcement) 1 else 0}"
                    )
                }

                if (hasGlobalEnforcement) {
                    item(
                        key = "home_global_enforcement"
                    ) {
                        GroupedListCard(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 4.dp
                            )
                        ) {
                            GroupedListRow(
                                primaryText = "Entire Network",
                                secondaryText =
                                    if (globalPolicy.action == "block") {
                                        "Global kill-switch is authoritative"
                                    } else {
                                        "Global allow override is authoritative"
                                    },
                                trailingContent = {
                                    StatusPill(
                                        text =
                                            if (globalPolicy.action == "block") {
                                                "Block All"
                                            } else {
                                                "Allow All"
                                            },
                                        tone =
                                            if (globalPolicy.action == "block") {
                                                PillTone.BLOCKED
                                            } else {
                                                PillTone.ALLOWED
                                            }
                                    )
                                },
                                onClick = {
                                    showGlobalSheet = true
                                }
                            )
                        }
                    }
                }

                attentionDevices.forEach {
                    device ->

                    item(
                        key =
                            "home_attention_${device.pdid}"
                    ) {

                        val status =
                            state
                                .effectiveStatusForDevice(
                                    device.pdid
                                )

                        val presentation =
                            AccessPresentationResolver
                                .resolve(
                                    device,
                                    status
                                )

                        GroupedListCard(
                            modifier =
                                Modifier.padding(
                                    horizontal =
                                        16.dp,
                                    vertical =
                                        4.dp
                                )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        14.dp
                                    )
                            ) {

                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Row(
                                        modifier =
                                            Modifier.weight(
                                                1f
                                            ),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        StatusDot(
                                            isOnline =
                                                device.online,
                                            isPaused =
                                                presentation
                                                    .isPaused
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.size(
                                                    8.dp
                                                )
                                        )

                                        Column {

                                            CupertinoText(
                                                text =
                                                    device.displayName,
                                                style =
                                                    HigTypography.headline,
                                                color =
                                                    LiasThemeColors.label
                                            )

                                            CupertinoText(
                                                text =
                                                    device.currentIP
                                                        .ifBlank {
                                                            if (
                                                                device.online
                                                            ) {
                                                                "Online"
                                                            } else {
                                                                "Offline"
                                                            }
                                                        },
                                                style =
                                                    HigTypography.caption,
                                                color =
                                                    LiasThemeColors
                                                        .tertiaryLabel
                                            )
                                        }
                                    }

                                    StatusPill(
                                        text =
                                            presentation.label,
                                        tone =
                                            presentation.tone
                                    )
                                }

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            10.dp
                                        )
                                )

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        )
                                ) {

                                    when {

                                        presentation
                                            .canResumePause ->

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

                                        presentation
                                            .canManageExtension ||
                                            presentation
                                                .canExtend ->

                                            HigButton(
                                                text =
                                                    if (
                                                        presentation
                                                            .canManageExtension
                                                    ) {
                                                        "Manage"
                                                    } else {
                                                        "Extend Access"
                                                    },
                                                onClick = {
                                                    activeDeviceForExtend =
                                                        device
                                                },
                                                style =
                                                    HigButtonStyle.Secondary,
                                                modifier =
                                                    Modifier.weight(
                                                        1f
                                                    )
                                            )

                                        presentation
                                            .canPause ->

                                            HigButton(
                                                text =
                                                    "Pause",
                                                onClick = {
                                                    activeDeviceForPause =
                                                        device
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
                                            "Details",
                                        onClick = {
                                            onNavigateToDeviceDetail(
                                                device.pdid
                                            )
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
                    }
                }

                activeTagEnforcements.forEach { (tag, status) ->
                    item(
                        key = "home_tag_enforcement_${tag.id}"
                    ) {
                        GroupedListCard(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 4.dp
                            )
                        ) {
                            GroupedListRow(
                                primaryText = tag.name,
                                secondaryText =
                                    "Group enforcement · ${status.source.replace('_', ' ')}",
                                trailingContent = {
                                    StatusPill(
                                        text =
                                            if (status.activeExtension != null) {
                                                "Extended"
                                            } else {
                                                "Blocked"
                                            },
                                        tone =
                                            if (status.activeExtension != null) {
                                                PillTone.ALLOWED
                                            } else {
                                                PillTone.BLOCKED
                                            }
                                    )
                                },
                                onClick = {
                                    onNavigateToTab(LiasScreen.Devices)
                                }
                            )
                        }
                    }
                }
            } else {
                item {
                    ListSectionHeader("Active Enforcements")
                }

                item {
                    GroupedListCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        GroupedListRow(
                            primaryText =
                                if (state.isInitialLoaded) {
                                    "No Active Enforcements"
                                } else {
                                    "Loading Enforcements"
                                },
                            secondaryText =
                                if (state.isInitialLoaded) {
                                    "All devices are currently operating under their server-authoritative default access state."
                                } else {
                                    "Waiting for authoritative LIAS effective-status data."
                                }
                        )
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
            onSave = {
                policy ->

                viewModel
                    .savePolicy(
                        policy
                    )

                showGlobalSheet =
                    false
            }
        )
    }

    activeDeviceForExtend
        ?.let {
            device ->

            val status =
                state
                    .effectiveStatusForDevice(
                        device.pdid
                    )

            val presentation =
                AccessPresentationResolver
                    .resolve(
                        device,
                        status
                    )

            if (
                presentation.canExtend ||
                presentation.canManageExtension
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
                        activeDeviceForExtend =
                            null
                    },
                    onConfirm = {
                        minutes ->

                        viewModel
                            .extendDeviceAccess(
                                device.pdid,
                                minutes
                            )

                        activeDeviceForExtend =
                            null
                    },
                    onCancelExtension =
                        if (
                            presentation
                                .canManageExtension
                        ) {

                            {
                                viewModel
                                    .cancelDeviceExtension(
                                        device.pdid
                                    )

                                activeDeviceForExtend =
                                    null
                            }

                        } else {
                            null
                        }
                )

            } else {

                activeDeviceForExtend =
                    null
            }
        }

    activeDeviceForPause
        ?.let {
            device ->

            val presentation =
                AccessPresentationResolver
                    .resolve(
                        device,
                        state
                            .effectiveStatusForDevice(
                                device.pdid
                            )
                    )

            if (
                presentation.canPause
            ) {

                PauseSheet(
                    targetLabel =
                        device.displayName,
                    onDismiss = {
                        activeDeviceForPause =
                            null
                    },
                    onConfirm = {
                        _ ->

                        viewModel
                            .pauseDeviceInternet(
                                device.pdid
                            )

                        activeDeviceForPause =
                            null
                    }
                )

            } else {

                activeDeviceForPause =
                    null
            }
        }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Column(
        modifier =
            modifier
                .background(
                    color =
                        LiasThemeColors
                            .secondaryBackground,
                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
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
                .semantics {
                    role =
                        Role.Button
                }
                .clickable(
                    onClick =
                        onClick
                )
                .padding(
                    vertical =
                        14.dp,
                    horizontal =
                        8.dp
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
                    .background(
                        color =
                            color,
                        shape =
                            RoundedCornerShape(
                                9.dp
                            )
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            CupertinoIcon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    Color.White,
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
                LiasThemeColors.label,
            textAlign =
                TextAlign.Center
        )
    }
}

@Composable
private fun MetricColumn(
    value: String,
    label: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        CupertinoText(
            text =
                value,
            style =
                HigTypography.title2,
            fontWeight =
                FontWeight.Bold,
            color =
                LiasThemeColors.label
        )

        CupertinoText(
            text =
                label,
            style =
                HigTypography.caption,
            color =
                LiasThemeColors
                    .secondaryLabel
        )
    }
}
