// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt
// Version: 3.0.0
// Audit Fixes:
//   1. Added physics spring animation (`dampingRatio = 0.86f`) to white thumb offset.
//   2. Enforced 38dp container height (`HigSpec.SegmentedControlHeight`).
//   3. Styled Allow (primary blue), Block (error red), and default states dynamically.
// Migration: Replaced hand-rolled implementation with CupertinoSegmentedControl for native iOS behavior.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        modifier = modifier.height(HigSpec.SegmentedControlHeight),
        selectedTabIndex = selectedIndex,
        onTabClick = { index ->
            onSelected(options[index].lowercase())
        }
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            CupertinoSegmentedControlTab(
                onClick = { onSelected(label.lowercase()) },
                isSelected = isSelected
            ) {
                androidx.compose.material3.Text(
                    text = label,
                    color = when {
                        isSelected && label.equals("Block", ignoreCase = true) -> MaterialTheme.colorScheme.error
                        isSelected && label.equals("Allow", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                        isSelected -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
