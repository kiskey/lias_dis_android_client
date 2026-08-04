// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt
// Version: 1.0.0
// Purpose: Modal bottom sheet for Creating and Editing Tags (GAP-C03).
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lias.remote.core.models.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorSheet(
    initialTag: Tag?,
    onDismiss: () -> Unit,
    onSave: (Tag) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var name by remember { mutableStateOf(initialTag?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialTag?.color ?: "#0071e3") }

    val presetColors = listOf(
        "#0071e3", "#5856d6", "#ff9500", "#ff2d55", 
        "#af52de", "#0a84ff", "#00c7be", "#32ade6", 
        "#30d158", "#ffcc00", "#a28b55", "#ff3b30"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialTag == null) "New Tag Group" else "Edit Tag Group",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tag Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Text("Badge Color", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                presetColors.forEach { colorHex ->
                    val color = Color(android.graphics.Color.parseColor(colorHex))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color, CircleShape)
                            .clickable { selectedColor = colorHex }
                    ) {
                        if (selectedColor.equals(colorHex, ignoreCase = true)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            Button(
                onClick = {
                    val finalId = initialTag?.id ?: name.lowercase().replace(" ", "_")
                    onSave(Tag(
                        id = finalId,
                        name = name,
                        color = selectedColor,
                        precedence = initialTag?.precedence ?: 50,
                        builtin = initialTag?.builtin ?: false
                    ))
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Tag")
            }
        }
    }
}
