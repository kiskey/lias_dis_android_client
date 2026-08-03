// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt
// Version: 1.0.0
// Purpose: The 3-button Global Access Switch (Allow / Schedule / Block).
//          Mirrors the Apple HIG segmented control from the web dashboard.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedControl(
    selectedAction: String,
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("allow", "schedule", "block")
    val labels = mapOf(
        "allow" to "Allow All",
        "schedule" to "Schedule",
        "block" to "Block All"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { action ->
                val isSelected = action == selectedAction
                val containerColor = when {
                    isSelected && action == "block" -> MaterialTheme.colorScheme.error
                    isSelected && action == "allow" -> MaterialTheme.colorScheme.primary
                    isSelected -> MaterialTheme.colorScheme.surface
                    else -> Color.Transparent
                }
                val contentColor = when {
                    isSelected -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Button(
                    onClick = { onActionSelected(action) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = labels[action] ?: action)
                }
            }
        }
    }
}
