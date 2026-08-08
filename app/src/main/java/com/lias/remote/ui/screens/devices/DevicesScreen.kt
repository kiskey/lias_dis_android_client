// ====================================================================
// File: DevicesScreen.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Device inventory grouped by tags. Swipe to pause/edit.
//          Preserves Device/Tag API contracts.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.MinutePickerSheet
import com.lias.remote.ui.components.SwipeAction
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
                onClick = {
                    editingTag = null
                    showTagEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HigSpec.FabSize)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Tag", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            state.tags.forEach { tag ->
                val devicesInTag = groupedDevices[tag.id] ?: emptyList()
                if (devicesInTag.isNotEmpty()) {
                    item(key = "header_${tag.id}") {
                        ListSectionHeader("${tag.name} · ${devicesInTag.size}")
                    }
                    item(key = "card_${tag.id}") {
                        GroupedListCard {
                            devicesInTag.forEachIndexed { index, device ->
                                val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }
                                
                                HigSwipeRow(
                                    trailingAction = SwipeAction(
                                        icon = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                        color = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        onTrigger = {
                                            if (isPaused) viewModel.unpauseInternet(device.pdid)
                                            else viewModel.pauseInternet(device.pdid)
                                        }
                                    )
                                ) {
                                    GroupedListRow(
                                        primaryText = device.displayName,
                                        secondaryText = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unknown" }}",
                                        trailingContent = { 
                                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
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

    activeDeviceForExtend?.let { device ->
        MinutePickerSheet(
            targetLabel = device.displayName,
            onDismiss = { activeDeviceForExtend = null },
            onConfirm = { minutes ->
                viewModel.extendDeviceAccess(device.pdid, minutes)
                activeDeviceForExtend = null
            }
        )
    }
}
