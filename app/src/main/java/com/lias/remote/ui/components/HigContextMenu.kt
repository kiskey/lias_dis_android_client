// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigContextMenu.kt
// Version: 1.0.0
// Purpose: Long-press gesture context menu wrapper component for Devices
//          and Schedules rows matching iOS contextual dropdown style.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class ContextMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HigContextMenu(
    items: List<ContextMenuItem>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = { expanded = true }
            )
    ) {
        content()

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(14.dp)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            item.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.label,
                                    tint = if (item.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (item.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        item.onClick()
                    }
                )
            }
        }
    }
}
