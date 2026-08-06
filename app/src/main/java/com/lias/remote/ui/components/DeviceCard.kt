// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/DeviceCard.kt
// Version: 2.1.0
// Audit Fixes:
//   1. Updated online status dot color to SystemGreenDark (never blue).
//   2. Migrated quick action buttons to HigButton primitives.
//   3. Styled discovered service tags with LiasThemeColors.fill.
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemGreenDark

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(HigSpec.CardCorner)
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
                        .size(HigSpec.StatusDotSize)
                        .background(
                            color = if (device.online) SystemGreenDark else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "MAC: ${device.currentMAC.ifBlank { "N/A" }} · IP: ${device.currentIP.ifBlank { "N/A" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Type: ${device.deviceType.ifBlank { "Unclassified" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (device.safeServices.isNotEmpty()) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    device.safeServices.take(3).forEach { service ->
                        Box(
                            modifier = Modifier
                                .background(LiasThemeColors.fill, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = service,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            // Tag Selection Drawer
            TextButton(
                onClick = { tagsExpanded = !tagsExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Assign Tags (${assignedTags.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                            modifier = Modifier.padding(2.dp)
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

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isInfra) {
                    HigButton(
                        text = if (isPaused) "Resume" else "Pause",
                        onClick = if (isPaused) onUnpauseClick else onPauseClick,
                        style = if (isPaused) HigButtonStyle.Secondary else HigButtonStyle.Danger,
                        modifier = Modifier.weight(1f)
                    )
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
