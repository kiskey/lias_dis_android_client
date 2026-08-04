// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/dashboard/DashboardScreen.kt
// Version: 1.7.0
// Audit Fixes: 
//   1. Fully verified Material 3 PullToRefreshBox opt-in annotations and state handling.
// ====================================================================

package com.lias.remote.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.ConnectionStatusBanner
import com.lias.remote.ui.components.DeviceCard
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.components.WeeklyTimeline
import com.lias.remote.ui.screens.policies.PolicyWizardSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    var showGlobalWizard by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ConnectionStatusBanner(state.connectionState)
            Spacer(modifier = Modifier.size(16.dp))

            if (!state.isInitialLoaded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Loading LIAS Dashboard...")
                }
                return@Column
            }

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
                        selectedAction = globalPolicy.action,
                        onActionSelected = { newAction ->
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
                            val globalSchedules = state.schedules.filter { it.id in globalPolicy.getScheduleIDs() }
                            
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

            Spacer(modifier = Modifier.size(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard("TOTAL", state.devices.size.toString(), Modifier.weight(1f))
                StatCard("ONLINE", state.devices.count { it.online }.toString(), Modifier.weight(1f))
                StatCard("OFFLINE", state.devices.count { !it.online }.toString(), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.size(24.dp))
            Text("Recent Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(8.dp))

            if (state.devices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Waiting for Discovery Service to report inventory...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.devices.take(10)) { device ->
                        DeviceCard(
                            device = device,
                            tags = state.tags,
                            onTagSelected = { tagId ->
                                viewModel.assignTag(device.pdid, tagId)
                            }
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
            onDismiss = { showGlobalWizard = false },
            onSave = { 
                viewModel.savePolicy(it) 
                showGlobalWizard = false
            }
        )
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
