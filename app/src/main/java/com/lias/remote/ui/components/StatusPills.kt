// ====================================================================
// File: StatusPills.kt
// Version: 3.2.0 (Cupertino Refactor)
// Purpose: Single source of truth for PillTone and StatusPill.
//          Uses CupertinoText. Resolves redeclaration conflicts.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val (bgColor, fgColor, icon) = when (tone) {
        PillTone.ALLOWED -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary, Icons.Filled.Check)
        PillTone.BLOCKED -> Triple(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error, Icons.Filled.Close)
        PillTone.SCHEDULED -> Triple(Color(0xFFFF9500).copy(alpha = 0.15f), Color(0xFFFF9500), Icons.Filled.Schedule)
        PillTone.PAUSED -> Triple(Color(0xFFFF9500).copy(alpha = 0.15f), Color(0xFFFF9500), Icons.Filled.Pause)
        PillTone.INFO -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary, Icons.Filled.Check)
        PillTone.WARN -> Triple(Color(0xFFFFCC00).copy(alpha = 0.15f), Color(0xFFFFCC00), Icons.Filled.Schedule)
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(12.dp))
        CupertinoText(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            fontWeight = FontWeight.W700
        )
    }
}
