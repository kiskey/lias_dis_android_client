// ====================================================================
// File: HigButton.kt
// Version: 3.2.0 (Cupertino Refactor)
// Purpose: Refactored to use io.github.alexzhirkevich.cupertino API.
//          CupertinoButton and CupertinoText.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoText

enum class HigButtonStyle {
    Primary, Secondary, Gray, Danger
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
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary
        )
        HigButtonStyle.Gray -> CupertinoButtonDefaults.tintedButtonColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(HigSpec.ButtonCorner))
    ) {
        CupertinoText(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (style == HigButtonStyle.Primary || style == HigButtonStyle.Danger) FontWeight.W600 else FontWeight.W500,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun HigTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    CupertinoButton(
        onClick = onClick,
        colors = CupertinoButtonDefaults.plainButtonColors(contentColor = color),
        modifier = modifier
    ) {
        CupertinoText(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W400,
            color = color
        )
    }
}
