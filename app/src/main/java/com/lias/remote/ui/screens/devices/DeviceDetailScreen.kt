// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt
// Version: 2.0.0
// Purpose: Pushed full-screen Device Detail view with hero header,
//          identity grouped list, service chips, and activity history logs.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.network.ApiResult
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.StatusPill

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceDetailScreen(
    pdid: String,
    viewModel: LiasViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val device = state.devices.find { it.pdid == pdid } ?: return

    var logs by remember { mutableStateOf<List<FlowLog>>(emptyList()) }
    var isLoadingLogs by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }

    val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }

    LaunchedEffect(pdid) {
        val result = viewModel.getDeviceLogs(pdid)
        if (result is ApiResult.Success) {
            logs = result.data
        }
        isLoadingLogs = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device.friendlyName.ifBlank { device.hostname.ifBlank { device.pdid } }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (isPaused) {
                                DropdownMenuItem(
                                    text = { Text("Resume Internet") },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.unpauseInternet(device.pdid)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Pause Internet (1 hr)") },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.pauseInternet(device.pdid)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = device.friendlyName.ifBlank { device.hostname },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.W800
                        )
                        Text(
                            text = if (device.online) "● Online now" else "● Offline",
                            color = if (device.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Identity Group
            item { ListSectionHeader("Identity") }
            item { GroupedListRow(primaryText = "Hostname", secondaryText = device.hostname.ifBlank { "N/A" }) }
            item { GroupedListRow(primaryText = "MAC Address", secondaryText = device.currentMAC.ifBlank { "N/A" }) }
            item { GroupedListRow(primaryText = "IP Address", secondaryText = device.currentIP.ifBlank { "N/A" }) }
            item { GroupedListRow(primaryText = "Vendor / Model", secondaryText = "${device.vendor.ifBlank { "Unknown" }} · ${device.model.ifBlank { "Unknown" }}") }
            item { GroupedListRow(primaryText = "Device Type", secondaryText = device.deviceType.ifBlank { "Unclassified" }) }

            // Services
            item { ListSectionHeader("Discovered Services") }
            item {
                if (device.safeServices.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        device.safeServices.forEach { service ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(service, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                } else {
                    Text("No services discovered.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Activity Logs
            item { ListSectionHeader("Activity — Last 100 events") }
            if (isLoadingLogs) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (logs.isEmpty()) {
                item {
                    Text("No recent activity logged.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(logs) { log ->
                    val isBlock = log.action == "block"
                    GroupedListRow(
                        primaryText = log.timestamp,
                        trailingContent = {
                            StatusPill(
                                text = log.action.uppercase(),
                                color = if (isBlock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                backgroundColor = if (isBlock) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        }
                    )
                }
            }
        }
    }
}
