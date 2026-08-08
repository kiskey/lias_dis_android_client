package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

enum class PillTone {
    ALLOWED, BLOCKED, SCHEDULED, PAUSED, INFO, WARN
}

@Composable
fun StatusPill(
    text: String,
    tone: PillTone,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor) = when (tone) {
        PillTone.ALLOWED -> LiasThemeColors.green.copy(alpha = 0.18f) to LiasThemeColors.green
        PillTone.BLOCKED -> LiasThemeColors.red.copy(alpha = 0.18f) to LiasThemeColors.red
        PillTone.SCHEDULED -> LiasThemeColors.orange.copy(alpha = 0.18f) to LiasThemeColors.orange
        PillTone.PAUSED -> LiasThemeColors.orange.copy(alpha = 0.18f) to LiasThemeColors.orange
        PillTone.INFO -> LiasThemeColors.blue.copy(alpha = 0.18f) to LiasThemeColors.blue
        PillTone.WARN -> LiasThemeColors.orange.copy(alpha = 0.18f) to LiasThemeColors.orange
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CupertinoText(
            text = text.uppercase(),
            style = HigTypography.caption,
            color = fgColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusDot(
    isOnline: Boolean,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dotColor = when {
        isPaused -> LiasThemeColors.orange
        isOnline -> LiasThemeColors.green
        else -> LiasThemeColors.tertiaryLabel
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(dotColor)
    )
}
