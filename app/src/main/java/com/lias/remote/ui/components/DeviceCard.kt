// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/DeviceCard.kt
// Version: 1.7.0
// Audit Fixes: 
//   1. Multi-tag selection: Selecting a specific tag strips fallback 'generic' tag.
//      Unchecking all tags restores 'generic'.
//   2. Clean Apple HIG action row with single-line layout and responsive button layout.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceCard(
    device: Device,
    tags: List<Tag>,
    isPaused: Boolean = false,
    onTagsSelected: (List<String>) -> Unit = {},
    onPauseClick: () -> Unit = {},
    onUnpauseClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDetailsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var tagsExpanded by remember { mutableStateOf(false) }
    val assignedTags = device.safeTags.ifEmpty { listOf("generic") }
    val isInfra = assignedTags.contains("infrastructure")

    val displayName = device.friendlyName.ifBlank { 
        device.hostname.ifBlank { 
            (device.vendor + " " + device.model).trim().ifBlank { 
                device.currentMAC.ifBlank { 
                    device.pdid 
                } 
            } 
        } 
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (device.online) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "MAC: ${device.currentMAC.ifBlank { "N/A" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "IP: ${device.currentIP.ifBlank { "N/A" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Type: ${device.deviceType.ifBlank { "Unclassified" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (device.safeServices.isNotEmpty()) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    device.safeServices.take(3).forEach { service ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = service,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.size(12.dp))

            // Multi-tag Assignment Drawer
            TextButton(
                onClick = { tagsExpanded = !tagsExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Assign Tags (${assignedTags.size})", style = MaterialTheme.typography.labelMedium)
            }

            if (tagsExpanded) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        val isChecked = assignedTags.contains(tag.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updated = assignedTags.filterNot { it == "generic" }.toMutableList()
                                    if (checked) {
                                        if (tag.id == "generic") {
                                            updated.clear()
                                            updated.add("generic")
                                        } else {
                                            if (!updated.contains(tag.id)) updated.add(tag.id)
                                        }
                                    } else {
                                        updated.remove(tag.id)
                                    }
                                    val finalTags = if (updated.isEmpty()) listOf("generic") else updated
                                    onTagsSelected(finalTags)
                                }
                            )
                            Text(tag.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Quick Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isInfra) {
                    if (isPaused) {
                        TextButton(
                            onClick = onUnpauseClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume")
                        }
                    } else {
                        TextButton(
                            onClick = onPauseClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }
                    }
                }

                IconButton(onClick = onRenameClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                IconButton(onClick = onDetailsClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Assignment, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
