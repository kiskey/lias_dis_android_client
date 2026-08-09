@file:OptIn(io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi::class)

// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt
// Version: 28.1.0
//
// Thin adapter around Compose-Cupertino's native sliding segmented
// control. Existing callers keep the same LIAS Remote API.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoSegmentedControl
import io.github.alexzhirkevich.cupertino.CupertinoSegmentedControlTab
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    if (options.isEmpty()) return

    val selectedIndex =
        options.indexOfFirst {
            it.equals(selectedOption, ignoreCase = true)
        }.coerceAtLeast(0)

    CupertinoSegmentedControl(
        selectedTabIndex = selectedIndex,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        paddingValues = PaddingValues(0.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val destructive =
                isDestructive &&
                    index == options.lastIndex

            CupertinoSegmentedControlTab(
                onClick = {
                    onOptionSelected(option)
                },
                isSelected = selected
            ) {
                CupertinoText(
                    text = option,
                    color = when {
                        destructive ->
                            LiasThemeColors.red
                        selected ->
                            LiasThemeColors.label
                        else ->
                            LiasThemeColors.secondaryLabel
                    }
                )
            }
        }
    }
}
