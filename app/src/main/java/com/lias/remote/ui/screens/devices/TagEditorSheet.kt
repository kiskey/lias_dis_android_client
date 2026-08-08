// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt
// Version: 3.1.0
// Purpose: Modal bottom sheet for creating/editing tag groups with iOS swatches.
// Audit Fixes:
//   1. Guarded built-in system tags against name edits to prevent backend rejection.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.theme.HigSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorSheet(
    initialTag: Tag?,
    onDismiss: () -> Unit,
    onSave: (Tag) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialTag?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialTag?.color ?: "#0A84FF") }
    val isBuiltin = initialTag?.builtin == true

    val presetColors = listOf(
        "#0A84FF", "#5856D6", "#FF9500", "#FF2D55",
        "#00C7BE", "#30D158", "#FFCC00", "#A28B55"
    )

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = if (initialTag == null) "New Tag" else if (isBuiltin) "Built-in Tag" else "Edit Tag",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        val finalId = initialTag?.id ?: name.lowercase().replace(" ", "_")
                        onSave(Tag(
                            id = finalId,
                            name = if (isBuiltin) (initialTag?.name ?: name) else name,
                            color = selectedColor,
                            precedence = initialTag?.precedence ?: 50,
                            builtin = isBuiltin
                        ))
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isBuiltin) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🔒 Built-in System Tag: Name is fixed by LIAS core. Badge color can be customized.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HigField(
                value = name,
                onValueChange = { if (!isBuiltin) name = it },
                label = "Tag Name",
                placeholder = "e.g. Nursery",
                enabled = !isBuiltin
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text("BADGE COLOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetColors.forEach { colorHex ->
                    val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                    val color = Color(android.graphics.Color.parseColor(colorHex))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
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
