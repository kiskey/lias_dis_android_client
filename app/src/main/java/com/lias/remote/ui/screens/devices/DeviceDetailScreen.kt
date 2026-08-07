// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt
// Version: 3.0.0
// Purpose: Device detail view formatted with native iOS HIG inset grouped cards.
// Audit Fixes:
//   1. Formatted device details and service list with iOS inset grouped surface cards.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lias.remote.core.util.ExtendHelper
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.MinutePickerSheet
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.SkeletonRow
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemGreenDark

@OptIn(ExperimentalLayoutApi::class)
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
    var showExtendSheet by remember { mutableStateOf(false) }

    val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }
    val effectiveStatus = viewModel.effectiveStatusFor(device.pdid)
    val canExtend = ExtendHelper.isExtendAvailable(effectiveStatus)
    val activeExtension = effectiveStatus.activeExtension

    LaunchedEffect(pdid) {
        val result = viewModel.getDeviceLogs(pdid)
        if (result is ApiResult.Success) {
            logs = result.data
        }
        isLoadingLogs = false
    }

    HigLargeTitleScaffold(
        title = "",
        navLeading = {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text("‹ Devices", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        navTrailing = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (canExtend || activeExtension != null) {
                        DropdownMenuItem(
                            text = { Text("Extend Access (1–120 min)") },
                            leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null, tint = SystemGreenDark) },
                            onClick = {
                                menuExpanded = false
                                showExtendSheet = true
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (isPaused) "Resume Internet" else "Pause Internet (1 hr)") },
                        leadingIcon = { Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            menuExpanded = false
                            if (isPaused) viewModel.unpauseInternet(device.pdid)
                            else viewModel.pauseInternet(device.pdid)
                        }
                    )
                }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = device.displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.W800
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (device.online) "● Online now" else "● Offline",
                            color = if (device.online) SystemGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (activeExtension != null) {
                            val left = ExtendHelper.minutesUntil(activeExtension.expiresAt)
                            Spacer(modifier = Modifier.height(6.dp))
                            StatusPill(
                                text = "Extended · ${left}m left",
                                tone = PillTone.Allowed,
                                modifier = Modifier.clickable { showExtendSheet = true }
                            )
                        }
                    }
                }
            }

            item { ListSectionHeader("Identity") }
            item {
                GroupedListCard {
                    GroupedListRow(primaryText = "Hostname", secondaryText = device.hostname.ifBlank { "N/A" }, showDivider = true)
                    GroupedListRow(primaryText = "MAC Address", secondaryText = device.currentMAC.ifBlank { "N/A" }, showDivider = true)
                    GroupedListRow(primaryText = "IP Address", secondaryText = device.currentIP.ifBlank { "N/A" }, showDivider = true)
                    GroupedListRow(primaryText = "Vendor / Model", secondaryText = "${device.vendor.ifBlank { "Unknown" }} · ${device.model.ifBlank { "Unknown" }}", showDivider = true)
                    GroupedListRow(primaryText = "Device Type", secondaryText = device.deviceType.ifBlank { "Unclassified" }, showDivider = false)
                }
            }

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
                                    .background(LiasThemeColors.fill, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = service,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    Text("No services discovered.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { ListSectionHeader("Activity — Last 100 events") }
            if (isLoadingLogs) {
                item { SkeletonRow() }
            } else if (logs.isEmpty()) {
                item {
                    Text("No recent activity logged.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                item {
                    GroupedListCard {
                        logs.forEachIndexed { index, log ->
                            val isBlock = log.action == "block"
                            GroupedListRow(
                                primaryText = log.timestamp,
                                trailingContent = {
                                    StatusPill(
                                        text = log.action.uppercase(),
                                        tone = if (isBlock) PillTone.Blocked else PillTone.Allowed
                                    )
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
        MinutePickerSheet(
            targetLabel = device.displayName,
            targetSubtitle = device.currentIP.ifBlank { device.pdid },
            currentExtension = activeExtension,
            onDismiss = { showExtendSheet = false },
            onConfirm = { minutes ->
                viewModel.extendDeviceAccess(device.pdid, minutes)
                showExtendSheet = false
            },
            onCancelExtension = if (activeExtension != null) {
                {
                    viewModel.cancelDeviceExtension(device.pdid)
                    showExtendSheet = false
                }
            } else null
        )
    }
}
