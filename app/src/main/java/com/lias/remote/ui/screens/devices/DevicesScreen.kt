// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt
// Version: 3.4.0
// Audit Fixes:
//   1. Added Cupertino StatusPill badge (PAUSED · [X]m) and secondary subtitle cue
//      when an individual device is in the paused state.
//   2. Preserved all swipe cancel 'X' options, MoveTagSheet, and Cupertino styling.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.core.util.ExtendHelper
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.ContextMenuItem
import com.lias.remote.ui.components.GroupedList
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigContextMenu
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSearchPill
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.MinutePickerSheet
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.SystemGreenDark

@Composable
fun DevicesScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showTagEditor by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var deviceToRename by remember { mutableStateOf<Device?>(null) }
    var deviceToMoveTag by remember { mutableStateOf<Device?>(null) }

    var activeDeviceForExtend by remember { mutableStateOf<Device?>(null) }
    var activeTagForExtend by remember { mutableStateOf<Tag?>(null) }

    val groupedDevices = remember(state.devices, searchQuery, state.tags) {
        val filtered = if (searchQuery.isBlank()) {
            state.devices
        } else {
            state.devices.filter { d ->
                d.displayName.contains(searchQuery, ignoreCase = true) ||
                d.currentMAC.contains(searchQuery, ignoreCase = true) ||
                d.currentIP.contains(searchQuery, ignoreCase = true)
            }
        }

        val map = mutableMapOf<String, MutableList<Device>>()
        state.tags.forEach { map[it.id] = mutableListOf() }

        filtered.forEach { d ->
            val assigned = d.safeTags.ifEmpty { listOf("generic") }
            assigned.forEach { tagId ->
                map.getOrPut(tagId) { mutableListOf() }.add(d)
            }
        }
        map
    }

    HigLargeTitleScaffold(
        title = "Devices",
        navTrailing = {
            IconButton(
                onClick = {
                    editingTag = null
                    showTagEditor = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Tag Group", tint = MaterialTheme.colorScheme.primary)
            }
        },
        searchField = {
            HigSearchPill(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search devices"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTag = null
                    showTagEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Tag Group", tint = Color.White)
            }
        }
    ) {
        GroupedList {
            state.tags.forEach { tag ->
                val devicesInTag = groupedDevices[tag.id] ?: emptyList()
                if (devicesInTag.isNotEmpty()) {
                    val tagStatus = viewModel.effectiveStatusForTag(tag.id)
                    val canExtendTag = ExtendHelper.isExtendAvailable(tagStatus)

                    item(key = "header_${tag.id}") {
                        ListSectionHeader(
                            text = "${tag.name} ${if (tag.id == "infrastructure") "🔒" else ""} · ${devicesInTag.size}",
                            trailingAction = if (canExtendTag) {
                                {
                                    IconButton(
                                        onClick = { activeTagForExtend = tag },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = "Extend Tag Access",
                                            tint = SystemGreenDark
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                    item(key = "card_${tag.id}") {
                        GroupedListCard {
                            devicesInTag.forEachIndexed { index, device ->
                                val pausePol = state.policies.find { it.id == "pol_pause_${device.pdid}" }
                                val isPaused = pausePol != null && pausePol.enabled
                                val pauseMinsLeft = pausePol?.expiresAt?.let { ExtendHelper.minutesUntil(it) } ?: 0

                                val devStatus = viewModel.effectiveStatusFor(device.pdid)
                                val canExtendDevice = ExtendHelper.isExtendAvailable(devStatus)

                                val contextMenuItems = buildList {
                                    add(
                                        ContextMenuItem(
                                            label = "View Details",
                                            icon = Icons.Default.Visibility,
                                            onClick = { onNavigateToDeviceDetail(device.pdid) }
                                        )
                                    )
                                    add(
                                        ContextMenuItem(
                                            label = "Move Tag Group",
                                            icon = Icons.Default.Sell,
                                            onClick = { deviceToMoveTag = device }
                                        )
                                    )
                                    if (canExtendDevice) {
                                        add(
                                            ContextMenuItem(
                                                label = "Extend Access (1–120 min)",
                                                icon = Icons.Default.HourglassTop,
                                                onClick = { activeDeviceForExtend = device }
                                            )
                                        )
                                    }
                                    add(
                                        ContextMenuItem(
                                            label = "Rename",
                                            icon = Icons.Default.Edit,
                                            onClick = { deviceToRename = device }
                                        )
                                    )
                                    add(
                                        ContextMenuItem(
                                            label = if (isPaused) "Resume Internet" else "Pause Internet (1 hr)",
                                            icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            isDestructive = !isPaused,
                                            onClick = {
                                                if (isPaused) viewModel.unpauseInternet(device.pdid)
                                                else viewModel.pauseInternet(device.pdid)
                                            }
                                        )
                                    )
                                }

                                HigContextMenu(
                                    items = contextMenuItems,
                                    onClick = { onNavigateToDeviceDetail(device.pdid) }
                                ) {
                                    HigSwipeRow(
                                        leadingAction = SwipeAction(
                                            label = "Move Tag",
                                            icon = Icons.Default.Sell,
                                            color = MaterialTheme.colorScheme.primary,
                                            onTrigger = {
                                                deviceToMoveTag = device
                                            }
                                        ),
                                        trailingAction = SwipeAction(
                                            label = if (isPaused) "Resume" else "Pause",
                                            icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            color = if (isPaused) SystemGreenDark else MaterialTheme.colorScheme.error,
                                            onTrigger = {
                                                if (isPaused) viewModel.unpauseInternet(device.pdid)
                                                else viewModel.pauseInternet(device.pdid)
                                            }
                                        )
                                    ) {
                                        GroupedListRow(
                                            primaryText = device.displayName,
                                            secondaryText = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unknown Vendor" }}${if (isPaused) " · ⏸ Paused" else ""}",
                                            leadingContent = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(HigSpec.StatusDotSize)
                                                        .background(
                                                            color = if (device.online) SystemGreenDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                            shape = CircleShape
                                                        )
                                                )
                                            },
                                            trailingContent = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (isPaused) {
                                                        StatusPill(
                                                            text = if (pauseMinsLeft > 0) "PAUSED · ${pauseMinsLeft}m" else "PAUSED",
                                                            tone = PillTone.Blocked
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronRight,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            showDivider = index < devicesInTag.size - 1,
                                            onClick = { onNavigateToDeviceDetail(device.pdid) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTagEditor) {
        TagEditorSheet(
            initialTag = editingTag,
            onDismiss = { showTagEditor = false },
            onSave = { tag ->
                if (editingTag == null) viewModel.createTag(tag)
                else viewModel.updateTag(tag)
                showTagEditor = false
            }
        )
    }

    deviceToMoveTag?.let { device ->
        MoveTagSheet(
            device = device,
            allTags = state.tags,
            onDismiss = { deviceToMoveTag = null },
            onConfirm = { tagIds ->
                viewModel.assignTags(device.pdid, tagIds)
                deviceToMoveTag = null
            }
        )
    }

    activeDeviceForExtend?.let { device ->
        val effectiveStatus = viewModel.effectiveStatusFor(device.pdid)
        MinutePickerSheet(
            targetLabel = device.displayName,
            targetSubtitle = device.currentIP.ifBlank { device.pdid },
            currentExtension = effectiveStatus.activeExtension,
            onDismiss = { activeDeviceForExtend = null },
            onConfirm = { minutes ->
                viewModel.extendDeviceAccess(device.pdid, minutes)
                activeDeviceForExtend = null
            },
            onCancelExtension = if (effectiveStatus.activeExtension != null) {
                {
                    viewModel.cancelDeviceExtension(device.pdid)
                    activeDeviceForExtend = null
                }
            } else null
        )
    }

    activeTagForExtend?.let { tag ->
        val tagDevices = groupedDevices[tag.id] ?: emptyList()
        val effectiveStatus = viewModel.effectiveStatusForTag(tag.id)
        MinutePickerSheet(
            targetLabel = tag.name,
            targetSubtitle = "${tagDevices.size} devices",
            currentExtension = effectiveStatus.activeExtension,
            onDismiss = { activeTagForExtend = null },
            onConfirm = { minutes ->
                viewModel.extendTagAccess(tag.id, minutes)
                activeTagForExtend = null
            },
            onCancelExtension = if (effectiveStatus.activeExtension != null) {
                {
                    viewModel.cancelTagExtension(tag.id)
                    activeTagForExtend = null
                }
            } else null
        )
    }

    deviceToRename?.let { device ->
        var newName by remember { mutableStateOf(device.displayName) }
        HigAlertDialog(
            onDismissRequest = { deviceToRename = null },
            title = { Text("Rename Device") },
            text = {
                Column {
                    Text("Enter a new friendly name:")
                    Spacer(modifier = Modifier.size(8.dp))
                    HigField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = "Friendly Name"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameDevice(device.pdid, newName)
                    }
                    deviceToRename = null
                }) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deviceToRename = null }) { Text("Cancel") }
            }
        )
    }
}
