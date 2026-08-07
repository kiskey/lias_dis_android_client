// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/StatusPill.kt
// Version: 2.1.0
// Audit Fixes:
//   1. Added PillTone enum (`Allowed`, `Blocked`, `Scheduled`, `Info`).
//   2. Derived semantic foreground/background colors automatically via theme.
//   3. Retained raw color/backgroundColor overload for full backward compatibility.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemGreenDark
import com.lias.remote.ui.theme.SystemOrangeDark
import com.lias.remote.ui.theme.SystemRedDark

enum class PillTone {
    Allowed,
    Blocked,
    Scheduled,
    Info
}

@Composable
fun StatusPill(
    text: String,
    tone: PillTone,
    modifier: Modifier = Modifier
) {
    val (fgColor, bgColor) = when (tone) {
        PillTone.Allowed -> MaterialTheme.colorScheme.primary to LiasThemeColors.pillGreenBg
        PillTone.Blocked -> MaterialTheme.colorScheme.error to LiasThemeColors.pillRedBg
        PillTone.Scheduled -> SystemOrangeDark to LiasThemeColors.pillOrangeBg
        PillTone.Info -> SystemBlueDark to LiasThemeColors.pillBlueBg
    }

    StatusPill(
        text = text,
        color = fgColor,
        backgroundColor = bgColor,
        modifier = modifier
    )
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = backgroundColor,
        shape = CircleShape,
        modifier = modifier.height(20.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = text.uppercase(),
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
