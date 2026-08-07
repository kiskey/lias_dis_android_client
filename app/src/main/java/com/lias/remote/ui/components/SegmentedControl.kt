// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt
// Version: 3.4.0
// Purpose: iOS segmented control wrapper using CupertinoSegmentedControl.
// Audit Fixes:
//   1. Added adaptive font scaling (14sp-16sp) based on option label lengths to ensure 3-pill controls do not crowd.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.lias.remote.ui.theme.HigSpec

@Composable
fun SegmentedControl(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = listOf("Allow", "Schedule", "Block")
) {
    val selectedIndex = options.indexOfFirst { it.equals(selected, ignoreCase = true) }.coerceAtLeast(0)

    val maxOptionLength = options.maxOfOrNull { it.length } ?: 0
    val textStyle = when {
        options.size >= 3 && maxOptionLength > 10 -> MaterialTheme.typography.labelLarge // 14sp w600
        options.size >= 3 -> MaterialTheme.typography.bodyLarge // 15sp w600
        else -> MaterialTheme.typography.titleLarge // 16sp w700
    }

    CupertinoSegmentedControl(
        selectedTabIndex = selectedIndex,
        modifier = modifier
            .fillMaxWidth()
            .height(HigSpec.SegmentedControlHeight)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            CupertinoSegmentedControlTab(
                isSelected = isSelected,
                onClick = { onSelected(label.lowercase()) }
            ) {
                Text(
                    text = label,
                    color = when {
                        isSelected && label.equals("Block", ignoreCase = true) -> MaterialTheme.colorScheme.error
                        isSelected && label.equals("Allow", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = textStyle,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
