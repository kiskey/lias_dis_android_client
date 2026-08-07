// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigButton.kt
// Version: 2.2.0
// Purpose: HIG full-width button supporting Primary, Secondary, and Danger styles.
// Audit Fixes:
//   1. Replaced direct CupertinoButtonColors constructor call with CupertinoButtonDefaults factory methods.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.robinpcrd.cupertino.CupertinoButton
import io.github.robinpcrd.cupertino.CupertinoButtonDefaults

enum class HigButtonStyle {
    Primary,
    Secondary,
    Danger
}

@Composable
fun HigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HigButtonStyle = HigButtonStyle.Primary,
    enabled: Boolean = true
) {
    val buttonColors = when (style) {
        HigButtonStyle.Primary -> CupertinoButtonDefaults.filledButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
        HigButtonStyle.Secondary -> CupertinoButtonDefaults.tintedButtonColors(
            containerColor = LiasThemeColors.fill,
            contentColor = MaterialTheme.colorScheme.primary
        )
        HigButtonStyle.Danger -> CupertinoButtonDefaults.filledButtonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = Color.White
        )
    }

    CupertinoButton(
        onClick = onClick,
        enabled = enabled,
        colors = buttonColors,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
