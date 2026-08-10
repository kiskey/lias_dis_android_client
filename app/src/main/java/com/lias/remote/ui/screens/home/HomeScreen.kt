// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt
//
// Purpose:
//   LIAS operational overview using server-authoritative access state.
// ====================================================================

package com.lias.remote.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.access.AccessPresentation
import com.lias.remote.ui.access.AccessPresentationResolver
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.navigation.LiasScreen
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.GlobalSwitchSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.Gear
import io.github.alexzhirkevich.cupertino.icons.outlined.House
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock
import io.github.alexzhirkevich.cupertino.icons.outlined.Pencil
import io.github.alexzhirkevich.cupertino.icons.outlined.Shield

@Composable
fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit,
    onNavigateToTab: (LiasScreen) -> Unit,
    onNavigateToIdentityReview: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()

    var showGlobalSheet by remember { mutableStateOf(false) }
    var activeDeviceForExtend by remember { mutableStateOf<Device?>(null) }

    val globalPolicy =
        state.policies.find { it.id == "global_default" }
            ?: Policy(
                id = "global_default",
                name = "Global Access",
                type = "global",
                targetID = "",
                action = "allow",
                priority = 0,
                enabled = true
            )

    val totalDevices = state.devices.size
    val onlineDevices = state.devices.count { it.online }
    val blockedDevices =
        state.devices.count { device ->
            state.effectiveStatusForDevice(device.pdid)
                ?.action
                ?.equals("block", ignoreCase = true) == true
        }
    val restrictedDevices = state.homeRestrictedDevices()
    val hasGlobalProtection = state.homeHasGlobalProtection(globalPolicy)
    val activeTagProtections =
        if (hasGlobalProtection) emptyList() else state.homeActiveTagProtections()
    val activePauseDevices =
        if (hasGlobalProtection) emptyList() else state.homeActivePauseDevices()
    val activeProtectionCount =
        activeTagProtections.size +
            activePauseDevices.size +
            if (hasGlobalProtection) 1 else 0

    HigLargeTitleScaffold(
        title = "Home",
        scrollState = scrollState
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item {
                ListSectionHeader("Network Overview")
            }

            item {
                GroupedListCard(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricColumn(totalDevices.toString(), "Total")
                        MetricColumn(onlineDevices.toString(), "Online")
                        MetricColumn(blockedDevices.toString(), "Blocked")
                    }

                    if (state.capabilities != null) {
                        HomeDivider()

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
                                    if (state.identityReview.pendingCount == 0) {
                                        "No pending possible matches"
                                    } else {
                                        "Review evidence before merging device records"
                                    },
                                trailingContent = {
                                    StatusPill(
                                        text =
                                            buildString {
                                                append(state.identityReview.pendingCount)
                                                if (state.identityReview.pendingHasMore) {
                                                    append("+")
                                                }
                                            },
                                        tone =
                                            if (state.identityReview.pendingCount == 0) {
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

            state.errorMessage?.let { message ->
                item {
                    HomeRefreshError(
                        message = message,
                        onRetry = viewModel::refresh
                    )
                }
            }

            item {
                ListSectionHeader(
                    if (activeProtectionCount > 0) {
                        "Active Protections · $activeProtectionCount"
                    } else {
                        "Active Protections"
                    }
                )
            }

            item {
                GroupedListCard(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (activeProtectionCount == 0) {
                        GroupedListRow(
                            primaryText =
                                if (state.isInitialLoaded) {
                                    "No Active Protections"
                                } else {
                                    "Loading Protections"
                                },
                            secondaryText =
                                if (state.isInitialLoaded) {
                                    "No global, group, or temporary pause protection is currently active."
                                } else {
                                    "Waiting for authoritative LIAS effective-status data."
                                }
                        )
                    } else {
                        if (hasGlobalProtection) {
                            GroupedListRow(
                                primaryText = "Entire Network",
                                secondaryText =
                                    if (globalPolicy.action.equals("block", true)) {
                                        "Global Access is blocking every non-infrastructure device"
                                    } else {
                                        "Global Access is allowing every non-infrastructure device"
                                    },
                                trailingContent = {
                                    StatusPill(
                                        text =
                                            if (globalPolicy.action.equals("block", true)) {
                                                "Block All"
                                            } else {
                                                "Allow All"
                                            },
                                        tone =
                                            if (globalPolicy.action.equals("block", true)) {
                                                PillTone.BLOCKED
                                            } else {
                                                PillTone.ALLOWED
                                            }
                                    )
                                },
                                leadingContent = {
                                    HomeIconBubble(
                                        icon = CupertinoIcons.Outlined.Shield,
                                        tint =
                                            if (globalPolicy.action.equals("block", true)) {
                                                LiasThemeColors.red
                                            } else {
                                                LiasThemeColors.green
                                            }
                                    )
                                },
                                showDivider = false,
                                onClick = { showGlobalSheet = true }
                            )
                        }

                        activeTagProtections.forEachIndexed { index, protection ->
                            val status = protection.status

                            GroupedListRow(
                                primaryText = protection.tag.name,
                                secondaryText = "Group policy · Internet blocked",
                                leadingContent = {
                                    HomeIconBubble(
                                        icon = protection.tag.homeIconKind().imageVector(),
                                        tint = LiasThemeColors.red
                                    )
                                },
                                trailingContent = {
                                    ProtectionPill(status)
                                },
                                showDivider =
                                    index < activeTagProtections.lastIndex ||
                                        activePauseDevices.isNotEmpty(),
                                onClick = {
                                    onNavigateToTab(LiasScreen.Devices)
                                }
                            )
                        }

                        activePauseDevices.forEachIndexed { index, device ->
                            val status = state.effectiveStatusForDevice(device.pdid)
                            val minutesLeft = status?.activeExtension?.minutesLeft ?: 0

                            GroupedListRow(
                                primaryText = device.displayName,
                                secondaryText =
                                    if (minutesLeft > 0) {
                                        "Temporary pause · $minutesLeft min remaining"
                                    } else {
                                        "Temporary pause currently in effect"
                                    },
                                leadingContent = {
                                    HomeIconBubble(
                                        icon = CupertinoIcons.Outlined.Clock,
                                        tint = LiasThemeColors.orange
                                    )
                                },
                                trailingContent = {
                                    StatusPill(
                                        text = "Paused",
                                        tone = PillTone.PAUSED
                                    )
                                },
                                showDivider = index < activePauseDevices.lastIndex,
                                onClick = {
                                    onNavigateToDeviceDetail(device.pdid)
                                }
                            )
                        }
                    }
                }
            }

            if (activeProtectionCount > 0) {
                item {
                    CupertinoText(
                        text = "Only protections currently in effect are shown.",
                        style = HigTypography.footnote,
                        color = LiasThemeColors.secondaryLabel,
                        modifier =
                            Modifier.padding(
                                start = 32.dp,
                                end = 16.dp,
                                top = 7.dp
                            )
                    )
                }
            }

            item {
                ListSectionHeader("Network Access")
            }

            item {
                GroupedListCard(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    GroupedListRow(
                        primaryText = "Global Access",
                        secondaryText = globalAccessSubtitle(globalPolicy),
                        leadingContent = {
                            CupertinoIcon(
                                imageVector = CupertinoIcons.Outlined.Shield,
                                contentDescription = null,
                                tint = LiasThemeColors.blue,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        trailingContent = {
                            GlobalAccessPill(globalPolicy)
                        },
                        onClick = { showGlobalSheet = true }
                    )
                }
            }

            item {
                ListSectionHeader(
                    if (restrictedDevices.isNotEmpty()) {
                        "Restricted Devices · ${restrictedDevices.size}"
                    } else {
                        "Restricted Devices"
                    }
                )
            }

            item {
                GroupedListCard(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (restrictedDevices.isEmpty()) {
                        GroupedListRow(
                            primaryText =
                                if (state.isInitialLoaded) {
                                    "No Restricted Devices"
                                } else {
                                    "Loading Device Status"
                                },
                            secondaryText =
                                if (state.isInitialLoaded) {
                                    "No individual device pause or block is currently active."
                                } else {
                                    "Waiting for authoritative LIAS effective-status data."
                                }
                        )
                    } else {
                        restrictedDevices.forEachIndexed { index, device ->
                            val status = state.effectiveStatusForDevice(device.pdid)
                            val presentation =
                                AccessPresentationResolver.resolve(device, status)
                            val primaryTag = device.homePrimaryTag(state.tags)

                            RestrictedDeviceRow(
                                device = device,
                                presentation = presentation,
                                primaryTag = primaryTag,
                                showDivider = index < restrictedDevices.lastIndex,
                                onResume = {
                                    viewModel.unpauseDeviceInternet(device.pdid)
                                },
                                onExtend = {
                                    activeDeviceForExtend = device
                                },
                                onDetails = {
                                    onNavigateToDeviceDetail(device.pdid)
                                }
                            )
                        }
                    }
                }
            }

            if (restrictedDevices.isNotEmpty()) {
                item {
                    CupertinoText(
                        text =
                            "${restrictedDevices.size} of $totalDevices devices currently restricted.",
                        style = HigTypography.footnote,
                        color = LiasThemeColors.secondaryLabel,
                        modifier =
                            Modifier.padding(
                                start = 32.dp,
                                end = 16.dp,
                                top = 7.dp,
                                bottom = 12.dp
                            )
                    )
                }
            } else {
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }

    if (showGlobalSheet) {
        GlobalSwitchSheet(
            currentPolicy = globalPolicy,
            onDismiss = { showGlobalSheet = false },
            onSave = { policy ->
                viewModel.savePolicy(policy)
                showGlobalSheet = false
            }
        )
    }

    activeDeviceForExtend?.let { device ->
        val status = state.effectiveStatusForDevice(device.pdid)
        val presentation = AccessPresentationResolver.resolve(device, status)

        if (presentation.canExtend || presentation.canManageExtension) {
            ExtendAccessSheet(
                targetLabel = device.displayName,
                targetSubtitle = device.currentIP.ifBlank { device.pdid },
                currentExtension = status?.activeExtension,
                onDismiss = { activeDeviceForExtend = null },
                onConfirm = { minutes ->
                    viewModel.extendDeviceAccess(device.pdid, minutes)
                    activeDeviceForExtend = null
                },
                onCancelExtension =
                    if (presentation.canManageExtension) {
                        {
                            viewModel.cancelDeviceExtension(device.pdid)
                            activeDeviceForExtend = null
                        }
                    } else {
                        null
                    }
            )
        } else {
            activeDeviceForExtend = null
        }
    }
}

@Composable
private fun RestrictedDeviceRow(
    device: Device,
    presentation: AccessPresentation,
    primaryTag: Tag?,
    showDivider: Boolean,
    onResume: () -> Unit,
    onExtend: () -> Unit,
    onDetails: () -> Unit
) {
    Column {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeIconBubble(
                        icon = primaryTag.homeIconKind().imageVector(),
                        tint = homeTagAccent(primaryTag)
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    Column {
                        CupertinoText(
                            text = device.displayName,
                            style = HigTypography.headline,
                            color = LiasThemeColors.label
                        )
                        CupertinoText(
                            text =
                                device.currentIP.ifBlank {
                                    if (device.online) "Online" else "Offline"
                                },
                            style = HigTypography.caption,
                            color = LiasThemeColors.tertiaryLabel
                        )
                    }
                }

                StatusPill(
                    text = presentation.label,
                    tone = presentation.tone
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    presentation.canResumePause ->
                        HigButton(
                            text = "Resume",
                            onClick = onResume,
                            style = HigButtonStyle.Primary,
                            modifier = Modifier.weight(1f)
                        )

                    presentation.canExtend ->
                        HigButton(
                            text = "Extend Access",
                            onClick = onExtend,
                            style = HigButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                }

                HigButton(
                    text = "Details",
                    onClick = onDetails,
                    style = HigButtonStyle.Gray,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showDivider) {
            HomeDivider()
        }
    }
}

@Composable
private fun HomeRefreshError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    color = LiasThemeColors.orange.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
    ) {
        CupertinoText(
            text = "Some LIAS data could not be refreshed.",
            style = HigTypography.headline,
            color = LiasThemeColors.label
        )
        CupertinoText(
            text = message,
            style = HigTypography.subheadline,
            color = LiasThemeColors.secondaryLabel
        )
        Spacer(modifier = Modifier.height(8.dp))
        HigButton(
            text = "Retry",
            onClick = onRetry,
            style = HigButtonStyle.Secondary
        )
    }
}

@Composable
private fun GlobalAccessPill(
    policy: Policy
) {
    StatusPill(
        text =
            when (policy.action.lowercase()) {
                "block" -> "Block All"
                "schedule" -> "Scheduled"
                else -> "Allow All"
            },
        tone =
            when (policy.action.lowercase()) {
                "block" -> PillTone.BLOCKED
                "schedule" -> PillTone.SCHEDULED
                else -> PillTone.ALLOWED
            }
    )
}

private fun globalAccessSubtitle(
    policy: Policy
): String =
    when (policy.action.lowercase()) {
        "block" -> "Block All"
        "schedule" -> "Schedule"
        else -> "Allow All"
    }

@Composable
private fun ProtectionPill(
    status: EffectiveStatus
) {
    val isExtended = status.activeExtension?.reasonTag.equals(
        "extend_access",
        ignoreCase = true
    )

    StatusPill(
        text =
            when {
                isExtended -> "Extended"
                status.action.equals("block", ignoreCase = true) -> "Blocked"
                else -> "Allowed"
            },
        tone =
            if (status.action.equals("block", ignoreCase = true)) {
                PillTone.BLOCKED
            } else {
                PillTone.ALLOWED
            }
    )
}

@Composable
private fun HomeIconBubble(
    icon: ImageVector,
    tint: Color
) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .background(
                    color = tint.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
        contentAlignment = Alignment.Center
    ) {
        CupertinoIcon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun homeTagAccent(
    tag: Tag?
): Color {
    val fallback = LiasThemeColors.blue
    val encoded = tag?.color?.trim().orEmpty()

    return if (encoded.isBlank()) {
        fallback
    } else {
        runCatching {
            Color(android.graphics.Color.parseColor(encoded))
        }.getOrDefault(fallback)
    }
}

private fun HomeTagIconKind.imageVector(): ImageVector =
    when (this) {
        HomeTagIconKind.SHIELD -> CupertinoIcons.Outlined.Shield
        HomeTagIconKind.LOCK -> CupertinoIcons.Outlined.Lock
        HomeTagIconKind.HOUSE -> CupertinoIcons.Outlined.House
        HomeTagIconKind.IPHONE -> CupertinoIcons.Outlined.Iphone
        HomeTagIconKind.GEAR -> CupertinoIcons.Outlined.Gear
        HomeTagIconKind.CLOCK -> CupertinoIcons.Outlined.Clock
        HomeTagIconKind.PENCIL -> CupertinoIcons.Outlined.Pencil
    }

@Composable
private fun HomeDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(start = 16.dp)
                .background(LiasThemeColors.separator)
    )
}

@Composable
private fun MetricColumn(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CupertinoText(
            text = value,
            style = HigTypography.title2,
            fontWeight = FontWeight.Bold,
            color = LiasThemeColors.label
        )
        CupertinoText(
            text = label,
            style = HigTypography.caption,
            color = LiasThemeColors.secondaryLabel
        )
    }
}
