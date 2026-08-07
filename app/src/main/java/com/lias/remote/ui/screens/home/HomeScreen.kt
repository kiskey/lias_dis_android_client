// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt
// Version: 3.1.0
// Purpose: Home Dashboard Screen with full-width 48dp segmented control hero card.
// Audit Fixes:
//   1. Ensured GlobalSwitchHero fills 100% available card width for thumb control.
// ====================================================================

package com.lias.remote.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.util.ExtendHelper
import com.lias.remote.core.util.ScheduleProjection
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.ConnectionStatusBanner
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.MiniWeekStrip
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.components.SkeletonGroupedList
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.screens.policies.PolicyWizardSheet
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.SystemGreenDark
import java.util.Calendar
import java.util.TimeZone

data class ActiveEnforcementItem(
    val policyName: String,
    val targetName: String,
    val targetColor: Color,
    val scheduleName: String,
    val action: String,
    val timezone: String = "UTC",
    val isDST: Boolean = false,
    val isGlobal: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var showGlobalWizard by remember { mutableStateOf(false) }

    val activeEnforcements = remember(state.policies, state.schedules, state.tags, state.devices) {
        computeActiveEnforcements(state.policies, state.schedules, state.tags, state.devices)
    }

    HigLargeTitleScaffold(
        title = "Home"
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { ConnectionStatusBanner(state.connectionState) }

                if (!state.isInitialLoaded && state.devices.isEmpty()) {
                    item { SkeletonGroupedList(count = 4) }
                } else {
                    item {
                        GlobalSwitchHero(
                            globalPolicy = state.policies.find { it.id == "global_default" },
                            schedules = state.schedules,
                            onSavePolicy = { viewModel.savePolicy(it) },
                            onManageSchedules = { showGlobalWizard = true }
                        )
                    }

                    if (activeEnforcements.isNotEmpty()) {
                        item {
                            Column {
                                ListSectionHeader("Active Enforcements")
                                ActiveEnforcementsList(items = activeEnforcements)
                            }
                        }
                    }

                    item { MetricsRow(devices = state.devices) }

                    item { ListSectionHeader("Recent Devices") }

                    item {
                        GroupedListCard {
                            state.devices.take(8).forEachIndexed { index, device ->
                                GroupedListRow(
                                    primaryText = device.displayName,
                                    secondaryText = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unknown Vendor" }}",
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    color = if (device.online) SystemGreenDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                )
                                        )
                                    },
                                    trailingContent = {
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    showDivider = index < state.devices.take(8).size - 1,
                                    onClick = { onNavigateToDeviceDetail(device.pdid) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGlobalWizard) {
        PolicyWizardSheet(
            viewModel = viewModel,
            initialPolicy = state.policies.find { it.id == "global_default" },
            tags = state.tags,
            schedules = state.schedules,
            existingPolicies = state.policies,
            onDismiss = { showGlobalWizard = false },
            onSave = { 
                viewModel.savePolicy(it) 
                showGlobalWizard = false
            }
        )
    }
}

@Composable
private fun GlobalSwitchHero(
    globalPolicy: Policy?,
    schedules: List<Schedule>,
    onSavePolicy: (Policy) -> Unit,
    onManageSchedules: () -> Unit
) {
    val pol = globalPolicy ?: Policy(id = "global_default", name = "Global Access Switch", type = "global", action = "schedule")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HigSpec.CardCorner)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(HigSpec.CardCorner)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("All Access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Master switch for every managed device", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            SegmentedControl(
                selected = pol.action,
                onSelected = { newAction ->
                    onSavePolicy(pol.copy(action = newAction))
                },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(
                visible = pol.action == "schedule",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    val attachedSchedules = schedules.filter { it.id in pol.resolveScheduleIDs() }
                    if (attachedSchedules.isNotEmpty()) {
                        MiniWeekStrip(schedules = attachedSchedules)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manage Schedules",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable { onManageSchedules() }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveEnforcementsList(items: List<ActiveEnforcementItem>) {
    GroupedListCard {
        items.forEachIndexed { index, item ->
            val isBlock = item.action == "block"
            GroupedListRow(
                primaryText = item.targetName,
                secondaryText = item.scheduleName,
                leadingContent = {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(item.targetColor))
                },
                trailingContent = {
                    StatusPill(
                        text = if (item.isGlobal) "Global " + item.action else item.action,
                        tone = if (isBlock) PillTone.Blocked else PillTone.Allowed
                    )
                },
                showDivider = index < items.size - 1
            )
        }
    }
}

@Composable
private fun MetricsRow(devices: List<Device>) {
    val total = devices.size
    val online = devices.count { it.online }
    val offline = total - online

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard("TOTAL", total.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        MetricCard("ONLINE", online.toString(), SystemGreenDark, Modifier.weight(1f))
        MetricCard("OFFLINE", offline.toString(), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(label: String, value: String, textColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = textColor, fontWeight = FontWeight.W800)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun computeActiveEnforcements(
    policies: List<Policy>,
    schedules: List<Schedule>,
    tags: List<Tag>,
    devices: List<Device>
): List<ActiveEnforcementItem> {
    val items = mutableListOf<ActiveEnforcementItem>()

    val globalPol = policies.find { it.id == "global_default" }
    if (globalPol != null && globalPol.enabled) {
        if (globalPol.action == "block") {
            items.add(
                ActiveEnforcementItem(
                    policyName = "Global Kill-Switch",
                    targetName = "Entire Network",
                    targetColor = Color.Red,
                    scheduleName = "Vacation Mode / Block All",
                    action = "block",
                    isGlobal = true
                )
            )
            return items
        } else if (globalPol.action == "allow") {
            items.add(
                ActiveEnforcementItem(
                    policyName = "Global Allow Override",
                    targetName = "Entire Network",
                    targetColor = Color.Green,
                    scheduleName = "Allow All Active",
                    action = "allow",
                    isGlobal = true
                )
            )
            return items
        }
    }

    policies.forEach { p ->
        if (!p.enabled || p.type == "global" || p.targetID == "infrastructure") return@forEach

        if (p.expiresAt != null) {
            val minsLeft = ExtendHelper.minutesUntil(p.expiresAt)
            if (minsLeft <= 0) return@forEach
        }

        val targetTag = tags.find { it.id == p.targetID }
        val targetName = if (p.type == "tag") (targetTag?.name ?: p.targetID) else (devices.find { it.pdid == p.targetID }?.displayName ?: p.targetID)
        val targetColor = if (p.type == "tag") parseColor(targetTag?.color) else Color(0xFF0A84FF)

        if (p.action == "allow") {
            val minsLeft = ExtendHelper.minutesUntil(p.expiresAt)
            val schedLabel = if (p.id.startsWith("pol_extend_")) "Extended Access (${minsLeft}m left)" else "Allow Always"
            items.add(
                ActiveEnforcementItem(
                    policyName = p.name,
                    targetName = targetName,
                    targetColor = targetColor,
                    scheduleName = schedLabel,
                    action = "allow"
                )
            )
        } else if (p.action == "block") {
            val minsLeft = ExtendHelper.minutesUntil(p.expiresAt)
            val schedLabel = if (p.id.startsWith("pol_pause_")) "Paused Internet (${minsLeft}m left)" else "Block Always"
            items.add(
                ActiveEnforcementItem(
                    policyName = p.name,
                    targetName = targetName,
                    targetColor = targetColor,
                    scheduleName = schedLabel,
                    action = "block"
                )
            )
        } else if (p.action == "schedule") {
            val schedIDs = p.resolveScheduleIDs()
            val attachedScheds = schedules.filter { it.id in schedIDs }

            for (s in attachedScheds) {
                val activeAction = evaluateScheduleActiveAction(s)
                if (activeAction != null) {
                    val isDST = try {
                        TimeZone.getTimeZone(s.timezone).inDaylightTime(Calendar.getInstance().time)
                    } catch (e: Exception) { false }

                    items.add(
                        ActiveEnforcementItem(
                            policyName = p.name,
                            targetName = targetName,
                            targetColor = targetColor,
                            scheduleName = "${s.name} (${activeAction.uppercase()})",
                            action = activeAction,
                            timezone = s.timezone,
                            isDST = isDST
                        )
                    )
                    break
                }
            }
        }
    }
    return items
}

private fun evaluateScheduleActiveAction(s: Schedule): String? {
    try {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(s.timezone))
        val currentDayIdx = cal.get(Calendar.DAY_OF_WEEK) - 1
        val currentMin = currentDayIdx * 1440 + cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val segments = ScheduleProjection.projectSchedule(s)
        for (seg in segments) {
            if (currentMin >= seg.start && currentMin < seg.end) {
                return seg.action
            }
        }
    } catch (_: Exception) {}
    return null
}

private fun parseColor(colorHex: String?): Color {
    if (colorHex.isNullOrBlank()) return Color.Gray
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) { Color.Gray }
}
