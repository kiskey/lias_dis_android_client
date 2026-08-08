// ====================================================================
// File: HomeScreen.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Home Dashboard. Hero status card, Active Enforcements list,
//          and strict HIG layout. Preserves all policy/stats logic.
// ====================================================================

package com.lias.remote.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.theme.HigSpec

@Composable
fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    
    var showBlockConfirm by remember { mutableStateOf(false) }
    var pendingGlobalAction by remember { mutableStateOf("schedule") }

    val globalPolicy = state.policies.find { it.id == "global_default" } ?: Policy(
        id = "global_default", name = "Global Access Switch", type = "global", action = "schedule"
    )

    val total = state.devices.size
    val online = state.devices.count { it.online }
    val offline = total - online

    HigLargeTitleScaffold(
        title = "Home",
        scrollState = scrollState
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = it
        ) {
            // Hero Card
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    GroupedListCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("NETWORK STATUS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$online devices online", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("$offline offline · ${state.policies.size} rules active", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            SegmentedControl(
                                options = listOf("Allow", "Schedule", "Block"),
                                selectedOption = globalPolicy.action.replaceFirstChar { it.uppercase() },
                                onOptionSelected = { selection ->
                                    val action = selection.lowercase()
                                    if (action == "block") {
                                        pendingGlobalAction = action
                                        showBlockConfirm = true
                                    } else {
                                        viewModel.savePolicy(globalPolicy.copy(action = action, enabled = true))
                                    }
                                },
                                isDestructive = true,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }

            // Active Enforcements
            item { ListSectionHeader("Active Enforcements") }
            item {
                GroupedListCard {
                    state.policies.filter { it.enabled && it.action == "block" }.take(5).forEachIndexed { index, policy ->
                        val targetName = if (policy.type == "global") "Entire Network" else policy.targetID
                        GroupedListRow(
                            primaryText = policy.name,
                            secondaryText = targetName,
                            leadingContent = { 
                                Icon(Icons.Filled.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.error) 
                            },
                            trailingContent = { StatusPill(text = "Block", tone = PillTone.BLOCKED) },
                            showDivider = index < 4
                        )
                    }
                    if (state.policies.none { it.enabled && it.action == "block" }) {
                        GroupedListRow(primaryText = "No active enforcements", secondaryText = "All devices operating normally")
                    }
                }
            }

            // Quick Actions
            item { ListSectionHeader("Quick Actions") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HigButton(text = "Devices", onClick = { /* Navigate */ }, style = HigButtonStyle.Gray, modifier = Modifier.weight(1f))
                    HigButton(text = "Schedules", onClick = { /* Navigate */ }, style = HigButtonStyle.Gray, modifier = Modifier.weight(1f))
                }
            }

            // Recent Devices
            item { ListSectionHeader("Recent Devices") }
            item {
                GroupedListCard {
                    state.devices.take(5).forEachIndexed { index, device ->
                        GroupedListRow(
                            primaryText = device.displayName,
                            secondaryText = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unknown" }}",
                            leadingContent = { Icon(Icons.Filled.Devices, contentDescription = null) },
                            trailingContent = { 
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                            },
                            showDivider = index < state.devices.take(5).size - 1,
                            onClick = { onNavigateToDeviceDetail(device.pdid) }
                        )
                    }
                }
            }
        }
    }

    if (showBlockConfirm) {
        HigAlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = "Block All Devices?",
            message = "This will immediately block internet access for all non-infrastructure devices on your network.",
            confirmText = "Block All",
            onConfirm = {
                viewModel.savePolicy(globalPolicy.copy(action = pendingGlobalAction, enabled = true))
            },
            isDestructive = true
        )
    }
}
