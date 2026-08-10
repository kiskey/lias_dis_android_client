// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/StatusPills.kt
// Version: 21.0.0
//
// Purpose:
//   Compact access/device status indicators.
//
// Batch 21:
//   - Status text remains visible; color is supplementary only.
//   - TalkBack receives explicit status semantics.
//   - StatusDot exposes Online / Offline / Paused rather than relying
//     solely on green/orange/gray.
//   - Removes forced uppercase from visible status text to improve
//     readability at larger text sizes.
// ====================================================================

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

enum class PillTone {
    ALLOWED,
    BLOCKED,
    SCHEDULED,
    PAUSED,
    INFO,
    WARN
}

@Composable
fun StatusPill(
    text: String,
    tone: PillTone,
    modifier: Modifier = Modifier
) {

    val backgroundColor =
        when (
            tone
        ) {

            PillTone.ALLOWED ->
                LiasThemeColors.green
                    .copy(
                        alpha =
                            0.16f
                    )

            PillTone.BLOCKED ->
                LiasThemeColors.red
                    .copy(
                        alpha =
                            0.16f
                    )

            PillTone.SCHEDULED,
            PillTone.PAUSED,
            PillTone.WARN ->
                LiasThemeColors.orange
                    .copy(
                        alpha =
                            0.16f
                    )

            PillTone.INFO ->
                LiasThemeColors.blue
                    .copy(
                        alpha =
                            0.16f
                    )
        }

    val foregroundColor =
        when (
            tone
        ) {

            PillTone.ALLOWED ->
                LiasThemeColors.green

            PillTone.BLOCKED ->
                LiasThemeColors.red

            PillTone.SCHEDULED,
            PillTone.PAUSED,
            PillTone.WARN ->
                LiasThemeColors.orange

            PillTone.INFO ->
                LiasThemeColors.blue
        }

    val semanticState =
        when (
            tone
        ) {

            PillTone.ALLOWED ->
                "Allowed"

            PillTone.BLOCKED ->
                "Blocked"

            PillTone.SCHEDULED ->
                "Scheduled"

            PillTone.PAUSED ->
                "Paused"

            PillTone.INFO ->
                "Information"

            PillTone.WARN ->
                "Warning"
        }

    Row(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        999.dp
                    )
                )
                .background(
                    backgroundColor
                )
                .semantics(
                    mergeDescendants =
                        true
                ) {

                    stateDescription =
                        semanticState

                    contentDescription =
                        text
                }
                .padding(
                    horizontal =
                        10.dp,
                    vertical =
                        5.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                4.dp
            )
    ) {

        CupertinoText(
            text =
                text,
            style =
                HigTypography.caption,
            color =
                foregroundColor,
            fontWeight =
                FontWeight.SemiBold,
            maxLines =
                2
        )
    }
}

@Composable
fun StatusDot(
    isOnline: Boolean,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {

    val state =
        when {

            isPaused ->
                "Paused"

            isOnline ->
                "Online"

            else ->
                "Offline"
        }

    val color =
        when {

            isPaused ->
                LiasThemeColors.orange

            isOnline ->
                LiasThemeColors.green

            else ->
                LiasThemeColors
                    .tertiaryLabel
        }

    Box(
        modifier =
            modifier
                .size(
                    10.dp
                )
                .clip(
                    CircleShape
                )
                .background(
                    color
                )
                .semantics {

                    contentDescription =
                        "Device status"

                    stateDescription =
                        state
                }
    )
}
