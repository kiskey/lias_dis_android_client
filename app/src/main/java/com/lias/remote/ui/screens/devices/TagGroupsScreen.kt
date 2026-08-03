// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/TagGroupsScreen.kt
// Version: 1.0.0
// Purpose: Displays devices grouped by tags. Features collapsible
//          accordion sections for clean organization on mobile.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lias.remote.core.models.Device
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.DeviceCard
import com.lias.remote.core.models.Tag

@Composable
fun TagGroupsScreen(viewModel: LiasViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    // Group devices by their first tag
    val groupedDevices = remember(state.devices) {
        state.devices.groupBy { it.tags.firstOrNull() ?: "generic" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(state.tags, key = { it.id }) { tag ->
            val devicesInGroup = groupedDevices[tag.id] ?: emptyList()
            ExpandableTagGroup(
                tag = tag,
                devices = devicesInGroup,
                allTags = state.tags,
                onTagSelected = { pdid, tagId -> viewModel.assignTag(pdid, tagId) }
            )
        }
    }
}

@Composable
private fun ExpandableTagGroup(
    tag: Tag,
    devices: List<Device>,
    allTags: List<Tag>,
    onTagSelected: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "(${devices.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = "Expand",
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                // Grid layout doesn't play nicely inside LazyColumn without fixed height.
                // For a thin client, a simple Column or Row layout is safer.
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
