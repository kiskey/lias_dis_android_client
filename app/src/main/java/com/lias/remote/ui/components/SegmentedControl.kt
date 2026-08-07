// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt
// Version: 3.3.0
// Purpose: iOS segmented control wrapper using CupertinoSegmentedControl.
// Audit Fixes:
//   1. Increased height to 48dp and scaled font size to 16sp for Pixel 6a thumb accessibility.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.lias.remote.ui.theme.HigSpec
import io.github.robinpcrd.cupertino.CupertinoSegmentedControl
import io.github.robinpcrd.cupertino.CupertinoSegmentedControlTab

@Composable
fun SegmentedControl(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = listOf("Allow", "Schedule", "Block")
) {
    val selectedIndex = options.indexOfFirst { it.equals(selected, ignoreCase = true) }.coerceAtLeast(0)

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
                    style = MaterialTheme.typography.titleLarge, // 16sp w700
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}
