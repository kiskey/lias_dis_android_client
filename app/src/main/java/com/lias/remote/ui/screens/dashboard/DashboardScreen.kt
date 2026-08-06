// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/dashboard/DashboardScreen.kt
// Version: 1.16.0
// Audit Fixes: 
//   1. Aligned SegmentedControl call arguments to `selected` and `onSelected`
//      to fix Kotlin compiler argument mismatch errors.
//   2. Preserved HIG 2.0 presentation layer, active enforcements, and pull-to-refresh.
// ====================================================================

package com.lias.remote.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lias.remote.ui.components.DeviceCard
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.components.WeeklyTimeline
import com.lias.remote.ui.screens.devices.DeviceDetailsSheet
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
fun DashboardScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    var showGlobalWizard by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    var selectedDeviceForDetails by remember { mutableStateOf<Device?>(null) }
    var deviceToPause by remember { mutableStateOf<Device?>(null) }
    var deviceToUnpause by remember { mutableStateOf<Device?>(null) }
    var deviceToRename by remember { mutableStateOf<Device?>(null) }

    val activeEnforcements = remember(state.policies, state.schedules, state.tags, state.devices) {
        computeActiveEnforcements(state.policies, state.schedules, state.tags, state.devices)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ConnectionStatusBanner(state.connectionState)
            }

            if (!state.isInitialLoaded && state.devices.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Connecting to LIAS...")
                    }
                }
            } else {
                item {
                    Column {
                        Text("Active Enforcements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            "Live status of scheduled policies currently in effect",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        ActiveEnforcementsList(items = activeEnforcements)
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Global Access Switch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                "Master internet control across all managed devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            
                            val globalPolicy = state.policies.find { it.id == "global_default" } 
                                ?: Policy(id = "global_default", name = "Global", type = "global", action = "schedule")
                            
                            SegmentedControl(
                                selected = globalPolicy.action,
                                onSelected = { newAction ->
                                    viewModel.savePolicy(globalPolicy.copy(action = newAction))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedVisibility(
                                visible = globalPolicy.action == "schedule",
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    val globalSchedules = state.schedules.filter { it.id in globalPolicy.resolveScheduleIDs() }
                                    
                                    if (globalSchedules.isNotEmpty()) {
                                        Text(
                                            "Attached Schedules (${globalSchedules.size})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        WeeklyTimeline(schedules = globalSchedules)
                                    }
                                    
                                    Button(
                                        onClick = { showGlobalWizard = true },
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                                    ) {
                                        Text("Manage Schedules")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard("TOTAL", state.devices.size.toString(), Modifier.weight(1f))
                        StatCard("ONLINE", state.devices.count { it.online }.toString(), Modifier.weight(1f))
                        StatCard("OFFLINE", state.devices.count { !it.online }.toString(), Modifier.weight(1f))
                    }
                }

                item {
                    Text("Recent Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (state.devices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Waiting for Discovery Service to report inventory...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(state.devices.take(12), key = { it.pdid }) { device ->
                        val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }
                        DeviceCard(
                            device = device,
                            tags = state.tags,
                            isPaused = isPaused,
                            onTagsSelected = { tagIds -> viewModel.assignTags(device.pdid, tagIds) },
                            onPauseClick = { deviceToPause = device },
                            onUnpauseClick = { deviceToUnpause = device },
                            onRenameClick = { deviceToRename = device },
                            onDetailsClick = { selectedDeviceForDetails = device },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
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

    selectedDeviceForDetails?.let { device ->
        DeviceDetailsSheet(
            device = device,
            viewModel = viewModel,
            onDismiss = { selectedDeviceForDetails = null }
        )
    }

    deviceToPause?.let { device ->
        val name = device.friendlyName.ifBlank { device.hostname.ifBlank { device.pdid } }
        AlertDialog(
            onDismissRequest = { deviceToPause = null },
            title = { Text("Pause Internet") },
            text = { Text("Are you sure you want to pause internet access for $name for 1 hour?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.pauseInternet(device.pdid)
                    deviceToPause = null
                }) { Text("Pause") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToPause = null }) { Text("Cancel") }
            }
        )
    }

    deviceToUnpause?.let { device ->
        val name = device.friendlyName.ifBlank { device.hostname.ifBlank { device.pdid } }
        AlertDialog(
            onDismissRequest = { deviceToUnpause = null },
            title = { Text("Resume Internet") },
            text = { Text("Are you sure you want to resume internet access for $name?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unpauseInternet(device.pdid)
                    deviceToUnpause = null
                }) { Text("Unpause") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToUnpause = null }) { Text("Cancel") }
            }
        )
    }

    deviceToRename?.let { device ->
        var newName by remember { mutableStateOf(device.friendlyName.ifBlank { device.hostname }) }
        AlertDialog(
            onDismissRequest = { deviceToRename = null },
            title = { Text("Rename Device") },
            text = {
                Column {
                    Text("Enter a new friendly name:")
                    Spacer(modifier = Modifier.size(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameDevice(device.pdid, newName)
                    }
                    deviceToRename = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToRename = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ActiveEnforcementsList(items: List<ActiveEnforcementItem>) {
    if (items.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("No Active Schedules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("All devices are currently operating under default rules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBlock = item.action == "block"
                        Icon(
                            imageVector = if (isBlock) Icons.Filled.Block else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isBlock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (item.isGlobal) {
                                    if (isBlock) "Global Block Active" else "Global Allow Active"
                                } else {
                                    if (isBlock) "Internet Blocked" else "Internet Allowed"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(item.targetColor))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(item.targetName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(item.scheduleName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (item.isDST) {
                            Text("DST Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
