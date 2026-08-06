// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt
// Version: 2.0.0
// Purpose: HIG Devices screen sectioned by tag group headers with search bar,
//          FAB + Top Bar '+' button for tag editing, and pushed row navigation.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.GroupedList
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.SwipeActionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showTagEditor by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }

    // Multi-tag grouping: devices appear under every tag group they belong to
    val groupedDevices = remember(state.devices, searchQuery, state.tags) {
        val filtered = if (searchQuery.isBlank()) {
            state.devices
        } else {
            state.devices.filter { d ->
                d.hostname.contains(searchQuery, ignoreCase = true) ||
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devices", style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    IconButton(onClick = {
                        editingTag = null
                        showTagEditor = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Tag Group")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTag = null
                    showTagEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Tag Group")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search devices") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            GroupedList {
                state.tags.forEach { tag ->
                    val devicesInTag = groupedDevices[tag.id] ?: emptyList()
                    if (devicesInTag.isNotEmpty()) {
                        item(key = "header_${tag.id}") {
                            ListSectionHeader("${tag.name} ${if (tag.id == "infrastructure") "🔒" else ""} · ${devicesInTag.size}")
                        }
                        items(devicesInTag, key = { "${tag.id}_${it.pdid}" }) { device ->
                            val isPaused = state.policies.any { it.id == "pol_pause_${device.pdid}" }
                            
                            SwipeActionRow(
                                onSwipeLeft = {
                                    if (isPaused) viewModel.unpauseInternet(device.pdid)
                                    else viewModel.pauseInternet(device.pdid)
                                },
                                onSwipeRight = {
                                    editingTag = tag
                                    showTagEditor = true
                                }
                            ) {
                                GroupedListRow(
                                    primaryText = device.friendlyName.ifBlank { device.hostname.ifBlank { device.pdid } },
                                    secondaryText = "${device.currentIP.ifBlank { "No IP" }} · ${device.vendor.ifBlank { "Unknown Vendor" }}",
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    color = if (device.online) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    shape = CircleShape
                                                )
                                        )
                                    },
                                    trailingContent = {
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    },
                                    onClick = { onNavigateToDeviceDetail(device.pdid) }
                                )
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
}
