// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/TagGroupsScreen.kt
// Version: 2.0.0
// Audit Fixes:
//   1. Supports devices belonging to multiple tags by displaying devices in all assigned groups.
//   2. Smooth HIG scrollability and full action row handling.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.DeviceCard

@Composable
fun TagGroupsScreen(viewModel: LiasViewModel) {
    val state by viewModel.state.collectAsState()
    
    var showEditor by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }

    var selectedDeviceForDetails by remember { mutableStateOf<Device?>(null) }
    var deviceToPause by remember { mutableStateOf<Device?>(null) }
    var deviceToUnpause by remember { mutableStateOf<Device?>(null) }
    var deviceToRename by remember { mutableStateOf<Device?>(null) }

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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTag = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Tag Group")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search devices...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.size(16.dp))

            if (state.tags.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tags yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.tags, key = { it.id }) { tag ->
                    val devicesInGroup = groupedDevices[tag.id] ?: emptyList()
                    ExpandableTagGroup(
                        tag = tag,
                        devices = devicesInGroup,
                        allTags = state.tags,
                        policies = state.policies,
                        onTagsSelected = { pdid, tagIds -> viewModel.assignTags(pdid, tagIds) },
                        onPauseClick = { deviceToPause = it },
                        onUnpauseClick = { deviceToUnpause = it },
                        onRenameClick = { deviceToRename = it },
                        onDetailsClick = { selectedDeviceForDetails = it },
                        onEditClick = {
                            editingTag = tag
                            showEditor = true
                        },
                        onDeleteClick = {
                            tagToDelete = tag
                        }
                    )
                }
            }
        }
    }

    if (showEditor) {
        TagEditorSheet(
            initialTag = editingTag,
            onDismiss = { showEditor = false },
            onSave = { tag ->
                if (editingTag == null) viewModel.createTag(tag)
                else viewModel.updateTag(tag)
                showEditor = false
            }
        )
    }

    selectedDeviceForDetails?.let { device ->
        DeviceDetailsSheet(
            device = device,
            viewModel = viewModel,
            onDismiss = { selectedDeviceForDetails = null }
        )
    }

    deviceToPause?.let { device ->
        val name = device.friendlyName.ifBlank { device.hostname.ifBlank { device.pdid } }
        AlertDialog(
            onDismissRequest = { deviceToPause = null },
            title = { Text("Pause Internet") },
            text = { Text("Are you sure you want to pause internet access for $name for 1 hour?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.pauseInternet(device.pdid)
                    deviceToPause = null
                }) { Text("Pause") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToPause = null }) { Text("Cancel") }
            }
        )
    }

    deviceToUnpause?.let { device ->
        val name = device.friendlyName.ifBlank { device.hostname.ifBlank { device.pdid } }
        AlertDialog(
            onDismissRequest = { deviceToUnpause = null },
            title = { Text("Resume Internet") },
            text = { Text("Are you sure you want to resume internet access for $name?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unpauseInternet(device.pdid)
                    deviceToUnpause = null
                }) { Text("Unpause") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToUnpause = null }) { Text("Cancel") }
            }
        )
    }

    deviceToRename?.let { device ->
        var newName by remember { mutableStateOf(device.friendlyName.ifBlank { device.hostname }) }
        AlertDialog(
            onDismissRequest = { deviceToRename = null },
            title = { Text("Rename Device") },
            text = {
                Column {
                    Text("Enter a new friendly name:")
                    Spacer(modifier = Modifier.size(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameDevice(device.pdid, newName)
                    }
                    deviceToRename = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToRename = null }) { Text("Cancel") }
            }
        )
    }

    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete Tag") },
            text = { Text("Are you sure you want to delete the tag '${tag.name}'? Devices will revert to 'Generic'.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag.id)
                        tagToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExpandableTagGroup(
    tag: Tag,
    devices: List<Device>,
    allTags: List<Tag>,
    policies: List<com.lias.remote.core.models.Policy>,
    onTagsSelected: (String, List<String>) -> Unit,
    onPauseClick: (Device) -> Unit,
    onUnpauseClick: (Device) -> Unit,
    onRenameClick: (Device) -> Unit,
    onDetailsClick: (Device) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "ChevronRotation"
    )

    val tagColor = remember(tag.color) {
        try {
            Color(android.graphics.Color.parseColor(tag.color))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
                Spacer(modifier = Modifier.size(12.dp))
                
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                if (tag.id == "infrastructure") {
                    Icon(Icons.Filled.Lock, contentDescription = "Immune", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                }
                
                Text(
                    text = "(${devices.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.size(8.dp))

                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotationState },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.size(4.dp))

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Tag Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Tag") },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                        )
                        if (!tag.builtin) {
                            DropdownMenuItem(
                                text = { Text("Delete Tag") },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (devices.isEmpty()) {
                        Text(
                            text = "No devices in this tag group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        devices.forEach { device ->
                            val isPaused = policies.any { it.id == "pol_pause_${device.pdid}" }
                            DeviceCard(
                                device = device,
                                tags = allTags,
                                isPaused = isPaused,
                                onTagsSelected = { tagIds -> onTagsSelected(device.pdid, tagIds) },
                                onPauseClick = { onPauseClick(device) },
                                onUnpauseClick = { onUnpauseClick(device) },
                                onRenameClick = { onRenameClick(device) },
                                onDetailsClick = { onDetailsClick(device) }
                            )
                        }
                    }
                }
            }
        }
    }
}
