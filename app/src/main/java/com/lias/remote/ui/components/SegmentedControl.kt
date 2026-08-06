// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt
// Version: 2.0.1
// Audit Fixes:
//   1. Replaced `.width(1f / options.size)` with `.fillMaxWidth(1f / options.size)`
//      to fix Compose Float fraction vs Dp compiler type mismatch error.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedControl(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = listOf("Allow", "Schedule", "Block")
) {
    var animatedSelectedIndex by remember(selected) {
        mutableIntStateOf(
            options.indexOfFirst { it.equals(selected, ignoreCase = true) }.coerceAtLeast(0)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(9.dp)
            )
            .padding(2.dp)
    ) {
        // Sliding pill indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(1f / options.size)
                .padding(2.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(7.dp)
                )
                .align(Alignment.CenterStart)
                .animateContentSize()
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == animatedSelectedIndex
                val contentColor = when {
                    isSelected && label.equals("Block", ignoreCase = true) -> MaterialTheme.colorScheme.error
                    isSelected && label.equals("Allow", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                    isSelected -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            animatedSelectedIndex = index
                            onSelected(label.lowercase())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
