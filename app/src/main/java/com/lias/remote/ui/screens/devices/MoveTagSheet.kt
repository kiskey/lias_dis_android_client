// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt
// Version: 1.0.0
// Purpose: Cupertino HIG modal bottom sheet to switch or move a device
//          between built-in and custom tag groups.
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.theme.HigSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveTagSheet(
    device: Device,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (tagIds: List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val assignedTags = remember(device) {
        device.safeTags.ifEmpty { listOf("generic") }
    }

    val selectedTagIds = remember {
        mutableStateListOf<String>().apply { addAll(assignedTags) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = HigSpec.SheetCorner, topEnd = HigSpec.SheetCorner)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Move Tag Group",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        val finalTags = if (selectedTagIds.isEmpty()) listOf("generic") else selectedTagIds.toList()
                        onConfirm(finalTags)
                    }
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "SELECT TAG GROUP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GroupedListCard {
                allTags.forEachIndexed { index, tag ->
                    val isChecked = selectedTagIds.contains(tag.id)
                    val tagColor = try {
                        Color(android.graphics.Color.parseColor(tag.color))
                    } catch (_: Exception) {
                        Color.Gray
                    }

                    GroupedListRow(
                        primaryText = tag.name + if (tag.id == "infrastructure") " (Immune)" else "",
                        secondaryText = if (tag.builtin) "Built-in System Tag" else "Custom Tag",
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tagColor)
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (tag.id == "infrastructure") {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Immune",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                }
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        showDivider = index < allTags.size - 1,
                        onClick = {
                            if (tag.id == "generic") {
                                selectedTagIds.clear()
                                selectedTagIds.add("generic")
                            } else {
                                selectedTagIds.remove("generic")
                                if (isChecked) {
                                    selectedTagIds.remove(tag.id)
                                    if (selectedTagIds.isEmpty()) {
                                        selectedTagIds.add("generic")
                                    }
                                } else {
                                    if (!selectedTagIds.contains(tag.id)) {
                                        selectedTagIds.add(tag.id)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
