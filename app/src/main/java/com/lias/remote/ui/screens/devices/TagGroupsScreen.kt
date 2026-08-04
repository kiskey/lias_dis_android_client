// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/TagGroupsScreen.kt
// Version: 1.7.0
// Audit Fixes:
//   1. Defaulted tag group cards to collapsed state (`expanded = false`) on launch
//      to prevent long scrolling and visual clutter.
//   2. Made the entire tag header row clickable to toggle expand/collapse.
//   3. Added animated rotating chevron indicator (`ExpandMore`).
//   4. Separated Edit/Delete tag options into a distinct `MoreVert` (3-dots) menu.
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

    val groupedDevices = remember(state.devices, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            state.devices
        } else {
            state.devices.filter { d ->
                d.hostname.contains(searchQuery, ignoreCase = true) ||
                d.currentMAC.contains(searchQuery, ignoreCase = true) ||
                d.currentIP.contains(searchQuery, ignoreCase = true)
            }
        }
        filtered.groupBy { it.safeTags.firstOrNull() ?: "generic" }
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
                        onTagSelected = { pdid, tagId -> viewModel.assignTag(pdid, tagId) },
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
    onTagSelected: (String, String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Audit Fix: Default collapsed view on app launch (`expanded = false`)
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
            // Clickable Header Row: Toggles expand / collapse
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

                // Animated Rotating Chevron Indicator
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotationState },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.size(4.dp))

                // 3-Dots Action Menu (Edit / Delete)
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
            
            // Expandable Content Body
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
                            DeviceCard(
                                device = device,
                                tags = allTags,
                                onTagSelected = { tagId -> onTagSelected(device.pdid, tagId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
