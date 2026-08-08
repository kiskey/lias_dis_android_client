// ====================================================================
// File: DevicesScreen.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Integrated PauseSheet and ExtendAccessSheet. Added Undo
//          state hooks for tag assignments.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.*
import com.lias.remote.ui.theme.HigSpec

@Composable
fun DevicesScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    
    var showTagEditor by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var activeDeviceForExtend by remember { mutableStateOf<Device?>(null) }
    var activeDeviceForPause by remember { mutableStateOf<Device?>(null) }

    val groupedDevices = remember(state.devices, searchQuery, state.tags) {
        val filtered = if (searchQuery.isBlank()) state.devices else state.devices.filter {
            it.displayName.contains(searchQuery, true) || it.currentMAC.contains(searchQuery, true) || it.currentIP.contains(searchQuery, true)
        }
        val map = mutableMapOf<String, MutableList<Device>>()
        state.tags.forEach { map[it.id] = mutableListOf() }
        filtered.forEach { d ->
            val assigned = d.safeTags.ifEmpty { listOf("generic") }
            assigned.forEach { tagId -> map.getOrPut(tagId) { mutableListOf() }.add(d) }
        }
        map
    }

    HigLargeTitleScaffold(
        title = "Devices",
        scrollState = scrollState,
        searchPlaceholder = "Search by name, IP, or MAC",
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingTag = null; showTagEditor = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) { Icon(Icons.Filled.Add, "New Tag", tint = Color.White) }
        }
    ) { padding ->
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            state.tags.forEach { tag ->
                val devicesInTag = groupedDevices[tag.id] ?: emptyList()
                if (devicesInTag.isNotEmpty()) {
                    item(key = "header_${tag.id}") { ListSectionHeader("${tag.name} · ${devicesInTag.size}") }
                    item(key = "card_${tag.id}") {
                        GroupedListCard {
                            devicesInTag.forEachIndexed { index, device ->
                                val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }
                                
                                HigSwipeRow(
                                    trailingAction = SwipeAction(
                                        icon = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                        color = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        onTrigger = {
                                            if (isPaused) viewModel.unpauseDeviceInternet(device.pdid)
                                            else activeDeviceForPause = device
                                        }
                                    )
                                ) {
                                    GroupedListRow(
                                        primaryText = device.displayName,
                                        secondaryText = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unknown" }}",
                                        trailingContent = { Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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

    if (showTagEditor) {
        TagEditorSheet(
            initialTag = editingTag,
            onDismiss = { showTagEditor = false },
            onSave = { tag ->
                if (editingTag == null) viewModel.createTag(tag) else viewModel.updateTag(tag)
                showTagEditor = false
            }
        )
    }

    activeDeviceForExtend?.let { device ->
        val status = viewModel.effectiveStatusFor(device.pdid)
        ExtendAccessSheet(
            targetLabel = device.displayName,
            targetSubtitle = device.currentIP.ifBlank { device.pdid },
            currentExtension = status.activeExtension,
            onDismiss = { activeDeviceForExtend = null },
            onConfirm = { mins ->
                viewModel.extendDeviceAccess(device.pdid, mins)
                activeDeviceForExtend = null
            },
            onCancelExtension = if (status.activeExtension != null) {
                { viewModel.cancelDeviceExtension(device.pdid); activeDeviceForExtend = null }
            } else null
        )
    }

    activeDeviceForPause?.let { device ->
        PauseSheet(
            targetLabel = device.displayName,
            onDismiss = { activeDeviceForPause = null },
            onConfirm = { mins ->
                viewModel.pauseDeviceInternet(device.pdid, mins)
                activeDeviceForPause = null
            }
        )
    }
}
