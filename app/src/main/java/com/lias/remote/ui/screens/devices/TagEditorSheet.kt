// ====================================================================
// File: TagEditorSheet.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: Modal sheet for creating/editing tag groups. Preserves
//          Tag data model and API contract.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton

@Composable
fun TagEditorSheet(
    initialTag: Tag?,
    onDismiss: () -> Unit,
    onSave: (Tag) -> Unit
) {
    var name by remember { mutableStateOf(initialTag?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialTag?.color ?: "#0A84FF") }

    val presetColors = listOf("#0A84FF", "#5856D6", "#FF9500", "#FF2D55", "#00C7BE", "#30D158", "#FFCC00", "#8E8E93")

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(
                title = if (initialTag == null) "New Tag" else "Edit Tag",
                onCancel = onDismiss,
                trailingAction = {
                    HigTextButton(
                        text = "Save",
                        onClick = {
                            val finalId = initialTag?.id ?: name.lowercase().replace(" ", "_")
                            onSave(Tag(
                                id = finalId,
                                name = name,
                                color = selectedColor,
                                precedence = initialTag?.precedence ?: 50,
                                builtin = initialTag?.builtin ?: false
                            ))
                        }
                    )
                }
            )

            HigField(
                value = name,
                onValueChange = { name = it },
                label = "Tag Name",
                placeholder = "e.g. Nursery"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "BADGE COLOR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    presetColors.forEach { colorHex ->
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                            .border(4.dp, color, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            }
        }
    }
}
