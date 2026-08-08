package com.lias.remote.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
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
            containerColor = LiasThemeColors.blue,
            contentColor = Color.White
        )
        HigButtonStyle.Secondary -> CupertinoButtonDefaults.tintedButtonColors(
            containerColor = LiasThemeColors.fill2,
            contentColor = LiasThemeColors.blue
        )
        HigButtonStyle.Gray -> CupertinoButtonDefaults.tintedButtonColors(
            containerColor = LiasThemeColors.fill2,
            contentColor = LiasThemeColors.label
        )
        HigButtonStyle.Danger -> CupertinoButtonDefaults.filledButtonColors(
            containerColor = LiasThemeColors.red,
            contentColor = Color.White
        )
    }

    CupertinoButton(
        onClick = onClick,
        enabled = enabled,
        colors = buttonColors,
        modifier = modifier
            .defaultMinSize(minHeight = HigSpec.ButtonHeight)
            .clip(RoundedCornerShape(HigSpec.ButtonCorner))
    ) {
        CupertinoText(
            text = text,
            style = HigTypography.headline,
            fontWeight = FontWeight.SemiBold,
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
    val contentColor = if (isDestructive) LiasThemeColors.red else LiasThemeColors.blue

    CupertinoButton(
        onClick = onClick,
        colors = CupertinoButtonDefaults.plainButtonColors(contentColor = contentColor),
        modifier = modifier
    ) {
        CupertinoText(
            text = text,
            style = HigTypography.body,
            fontWeight = FontWeight.Normal,
            color = contentColor
        )
    }
}
