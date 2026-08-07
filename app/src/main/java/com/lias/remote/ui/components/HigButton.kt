// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigButton.kt
// Version: 2.1.0
// Purpose: HIG full-width button supporting Primary, Secondary, and Danger styles.
// Migration: Replaced Material3 Button with CupertinoButton for iOS HIG fidelity.
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
import io.github.robinpcrd.cupertino.CupertinoButtonColors

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
        HigButtonStyle.Primary -> CupertinoButtonColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
            foregroundColor = Color.White,
            disabledBackgroundColor = LiasThemeColors.fill,
            disabledForegroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        HigButtonStyle.Secondary -> CupertinoButtonColors(
            backgroundColor = LiasThemeColors.fill,
            foregroundColor = MaterialTheme.colorScheme.primary,
            disabledBackgroundColor = LiasThemeColors.fill,
            disabledForegroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        HigButtonStyle.Danger -> CupertinoButtonColors(
            backgroundColor = MaterialTheme.colorScheme.error,
            foregroundColor = Color.White,
            disabledBackgroundColor = LiasThemeColors.fill,
            disabledForegroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
