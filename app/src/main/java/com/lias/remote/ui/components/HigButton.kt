// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigButton.kt
// Version: 1.0.0
// Purpose: HIG full-width button (12dp corner, 48dp min-height, 16sp/700 font)
//          supporting Primary, Secondary, and Danger visual styles.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.LiasThemeColors

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
    val (containerColor, contentColor) = when (style) {
        HigButtonStyle.Primary -> MaterialTheme.colorScheme.primary to Color.White
        HigButtonStyle.Secondary -> LiasThemeColors.fill to MaterialTheme.colorScheme.primary
        HigButtonStyle.Danger -> MaterialTheme.colorScheme.error to Color.White
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = LiasThemeColors.fill,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
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
