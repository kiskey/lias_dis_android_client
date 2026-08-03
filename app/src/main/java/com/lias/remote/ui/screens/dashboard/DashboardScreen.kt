// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/dashboard/DashboardScreen.kt
// Version: 1.0.0
// Purpose: Main dashboard view. Displays the global switch, high-level
//          stats, and a grid of recent devices. Binds to LiasViewModel.
// ====================================================================

package com.lias.remote.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lias.remote.core.models.Policy
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.ConnectionStatusBanner
import com.lias.remote.ui.components.DeviceCard
import com.lias.remote.ui.components.SegmentedControl

@Composable
fun DashboardScreen(viewModel: LiasViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ConnectionStatusBanner(state.connectionState)
        Spacer(modifier = Modifier.size(16.dp))

        // Global Switch Banner
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
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        // Stats Row
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
