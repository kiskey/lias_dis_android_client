// ====================================================================
// File: HomeScreen.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Integrated GlobalSwitchSheet to replace inline dialogs.
// ====================================================================

package com.lias.remote.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.*

@Composable
fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    var showGlobalSheet by remember { mutableStateOf(false) }

    val globalPolicy = state.policies.find { it.id == "global_default" } ?: Policy(
        id = "global_default", name = "Global Access Switch", type = "global", action = "schedule"
    )

    HigLargeTitleScaffold(title = "Home", scrollState = scrollState) {
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = it) {
            // Hero Card
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    GroupedListCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("NETWORK STATUS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.devices.count { it.online }} devices online", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            
                            HigButton(
                                text = "Manage Global Switch",
                                onClick = { showGlobalSheet = true },
                                style = HigButtonStyle.Secondary,
                                modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Active Enforcements
            item { ListSectionHeader("Active Enforcements") }
            item {
                GroupedListCard {
                    val active = state.policies.filter { it.enabled && it.action == "block" }.take(5)
                    if (active.isEmpty()) {
                        GroupedListRow(primaryText = "No active enforcements", secondaryText = "All devices operating normally")
                    } else {
                        active.forEachIndexed { index, policy ->
                            GroupedListRow(
                                primaryText = policy.name,
                                secondaryText = if (policy.type == "global") "Entire Network" else policy.targetID,
                                leadingContent = { Icon(Icons.Filled.Pause, null, tint = MaterialTheme.colorScheme.error) },
                                trailingContent = { StatusPill(text = "Block", tone = PillTone.BLOCKED) },
                                showDivider = index < active.size - 1
                            )
                        }
                    }
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
                            leadingContent = { Icon(Icons.Filled.Devices, null) },
                            trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            showDivider = index < state.devices.take(5).size - 1,
                            onClick = { onNavigateToDeviceDetail(device.pdid) }
                        )
                    }
                }
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
}
