// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/ConnectionFeedback.kt
// Version: 4.0.0
//
// Purpose:
//   Shared, understated Cupertino-style connection feedback.
//
// Design:
//   - No Toasts for important connection errors.
//   - Feedback remains visible near the action that caused it.
//   - Uses semantic status colors already established by the project.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

@Composable
fun ConnectionFeedback(
    message: String?,
    verified: Boolean,
    modifier: Modifier = Modifier
) {
    if (
        message.isNullOrBlank()
    ) {
        return
    }

    val background =
        if (verified) {
            LiasThemeColors.green.copy(
                alpha = 0.12f
            )
        } else {
            LiasThemeColors.red.copy(
                alpha = 0.10f
            )
        }

    val foreground =
        if (verified) {
            LiasThemeColors.green
        } else {
            LiasThemeColors.red
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = background,
                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 11.dp
                ),
        horizontalArrangement =
            Arrangement.Start,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        CupertinoText(
            text = message,
            style =
                HigTypography.subheadline,
            color = foreground
        )
    }
}
