// ====================================================================
// File: DeviceDetailScreen.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Device specifics. Sticky action bar. Grouped identity cards.
//          Preserves all /logs and /effective-status API calls.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.screens.ExtendAccessSheet
import com.lias.remote.ui.screens.PauseSheet

@Composable
fun DeviceDetailScreen(
    pdid: String,
    viewModel: LiasViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    val device = state.devices.find { it.pdid == pdid } ?: return

    var logs by remember { mutableStateOf<List<FlowLog>>(emptyList()) }
    var isLoadingLogs by remember { mutableStateOf(true) }
    var showExtendSheet by remember { mutableStateOf(false) }
    var showPauseSheet by remember { mutableStateOf(false) }

    val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }

    LaunchedEffect(pdid) {
        val result = viewModel.getDeviceLogs(pdid)
        if (result is ApiResult.Success) logs = result.data
        isLoadingLogs = false
    }

    HigLargeTitleScaffold(
        title = "",
        scrollState = scrollState,
        navLeading = { HigTextButton(text = "‹ Devices", onClick = onBack) }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.padding(8.dp))
                    Text(device.displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.W700)
                    Text(
                        text = if (device.online) "● Online · ${device.currentIP}" else "● Offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (device.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    // Sticky Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isPaused) {
                            HigButton(
                                text = "Resume Internet",
                                onClick = { viewModel.unpauseDeviceInternet(device.pdid) },
                                style = HigButtonStyle.Primary,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            HigButton(
                                text = "Pause",
                                onClick = { showPauseSheet = true },
                                style = HigButtonStyle.Secondary,
                                modifier = Modifier.weight(1f)
                            )
                            HigButton(
                                text = "Extend Access",
                                onClick = { showExtendSheet = true },
                                style = HigButtonStyle.Primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Identity
            item { ListSectionHeader("Identity") }
            item {
                GroupedListCard {
                    GroupedListRow(primaryText = "Hostname", secondaryText = device.hostname.ifBlank { "N/A" }, showDivider = true)
                    GroupedListRow(primaryText = "MAC Address", secondaryText = device.currentMAC.ifBlank { "N/A" }, showDivider = true)
                    GroupedListRow(primaryText = "IP Address", secondaryText = device.currentIP.ifBlank { "N/A" }, showDivider = true)
                    GroupedListRow(primaryText = "Vendor", secondaryText = device.vendor.ifBlank { "Unknown" }, showDivider = true)
                    GroupedListRow(primaryText = "Device Type", secondaryText = device.deviceType.ifBlank { "Unclassified" })
                }
            }

            // Activity Log
            item { ListSectionHeader("Activity (Last 24h)") }
            item {
                GroupedListCard {
                    if (isLoadingLogs) {
                        GroupedListRow(primaryText = "Loading logs...")
                    } else if (logs.isEmpty()) {
                        GroupedListRow(primaryText = "No recent activity logged.")
                    } else {
                        logs.forEachIndexed { index, log ->
                            val isBlock = log.action == "block"
                            GroupedListRow(
                                primaryText = log.timestamp,
                                trailingContent = { 
                                    StatusPill(text = log.action, tone = if (isBlock) PillTone.BLOCKED else PillTone.ALLOWED) 
                                },
                                showDivider = index < logs.size - 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExtendSheet) {
        val status = viewModel.effectiveStatusFor(device.pdid)
        ExtendAccessSheet(
            targetLabel = device.displayName,
            targetSubtitle = device.currentIP.ifBlank { device.pdid },
            currentExtension = status.activeExtension,
            onDismiss = { showExtendSheet = false },
            onConfirm = { mins ->
                viewModel.extendDeviceAccess(device.pdid, mins)
                showExtendSheet = false
            },
            onCancelExtension = if (status.activeExtension != null) {
                { viewModel.cancelDeviceExtension(device.pdid); showExtendSheet = false }
            } else null
        )
    }

    if (showPauseSheet) {
        PauseSheet(
            targetLabel = device.displayName,
            onDismiss = { showPauseSheet = false },
            onConfirm = { mins ->
                viewModel.pauseDeviceInternet(device.pdid, mins)
                showPauseSheet = false
            }
        )
    }
}
