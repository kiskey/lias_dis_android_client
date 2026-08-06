// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt
// Version: 2.0.1
// Audit Fixes:
//   1. Added missing imports `expandVertically`, `shrinkVertically`, and `androidx.compose.foundation.lazy.items`.
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.lias.remote.core.util.ScheduleProjection
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.ConnectionStatusBanner
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.WeeklyTimeline
import com.lias.remote.ui.screens.policies.PolicyWizardSheet
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ConnectionStatusBanner(state.connectionState) }

            if (!state.isInitialLoaded && state.devices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // All Access Hero Switch
                item {
                    GlobalSwitchHero(
                        globalPolicy = state.policies.find { it.id == "global_default" },
                        schedules = state.schedules,
                        onSavePolicy = { viewModel.savePolicy(it) },
                        onManageSchedules = { showGlobalWizard = true }
                    )
                }

                // Active Enforcements Section
                item {
                    Column {
                        ListSectionHeader("Active Enforcements")
                        ActiveEnforcementsList(items = activeEnforcements)
                    }
                }

                // Metrics Row
                item { MetricsRow(devices = state.devices) }

                // Recent Devices List
                item { ListSectionHeader("Recent Devices") }

                items(state.devices.take(10), key = { it.pdid }) { device ->
                    GroupedListRow(
                        primaryText = device.friendlyName.ifBlank { device.hostname.ifBlank { device.pdid } },
                        secondaryText = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unknown Vendor" }}",
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (device.online) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        onClick = { onNavigateToDeviceDetail(device.pdid) }
                    )
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("All Access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Master switch for every managed device", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            SegmentedControl(
                selected = pol.action,
                onSelected = { newAction ->
                    onSavePolicy(pol.copy(action = newAction))
                }
            )

            AnimatedVisibility(
                visible = pol.action == "schedule",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    val attachedSchedules = schedules.filter { it.id in pol.resolveScheduleIDs() }
                    if (attachedSchedules.isNotEmpty()) {
                        WeeklyTimeline(schedules = attachedSchedules)
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
    if (items.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("No Active Schedules", style = MaterialTheme.typography.titleLarge)
                    Text("All devices operating under default rules", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBlock = item.action == "block"
                        StatusPill(
                            text = if (item.isGlobal) "Global " + item.action else item.action,
                            color = if (isBlock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            backgroundColor = if (isBlock) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(item.targetColor))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(item.targetName, style = MaterialTheme.typography.titleLarge)
                            }
                            Text(item.scheduleName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
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
        MetricCard("ONLINE", online.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        MetricCard("OFFLINE", offline.toString(), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(label: String, value: String, textColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
    if (globalPol != null && globalPol.action == "block") {
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
    } else if (globalPol != null && globalPol.action == "allow") {
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

    policies.forEach { p ->
        if (p.action == "schedule" && p.type != "global" && p.enabled) {
            val targetTag = tags.find { it.id == p.targetID }
            val targetName = if (p.type == "tag") (targetTag?.name ?: p.targetID) else (devices.find { it.pdid == p.targetID }?.hostname ?: p.targetID)
            val targetColor = if (p.type == "tag") parseColor(targetTag?.color) else Color.Gray

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
                            scheduleName = s.name,
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
