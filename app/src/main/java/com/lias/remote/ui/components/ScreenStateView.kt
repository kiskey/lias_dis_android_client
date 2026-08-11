// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/ScreenStateView.kt
// Version: 27.2.0
//
// Purpose:
//   Shared HIG-style loading / empty / error / stale presentation.
//
// Design:
//   Avoids Material indicators and dialogs.
//   Uses progressive disclosure:
//      title -> concise explanation -> optional recovery action.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

enum class ScreenStateTone {
    NORMAL,
    WARNING,
    ERROR
}

@Composable
fun ScreenStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    tone: ScreenStateTone = ScreenStateTone.NORMAL
) {
    val accent =
        when (tone) {
            ScreenStateTone.NORMAL ->
                LiasThemeColors.blue

            ScreenStateTone.WARNING ->
                LiasThemeColors.orange

            ScreenStateTone.ERROR ->
                LiasThemeColors.red
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth(0.16f)
                    .height(5.dp)
                    .background(
                        color =
                            accent.copy(
                                alpha = 0.7f
                            ),
                        shape =
                            RoundedCornerShape(
                                999.dp
                            )
                    )
        )

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        CupertinoText(
            text = title,
            style =
                HigTypography.headline,
            fontWeight =
                FontWeight.SemiBold,
            color =
                LiasThemeColors.label,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        CupertinoText(
            text = message,
            style =
                HigTypography.body,
            color =
                LiasThemeColors.secondaryLabel,
            textAlign =
                TextAlign.Center
        )

        if (
            actionText != null &&
            onAction != null
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            HigButton(
                text = actionText,
                onClick = onAction,
                style =
                    if (
                        tone ==
                        ScreenStateTone.ERROR
                    ) {
                        HigButtonStyle.Secondary
                    } else {
                        HigButtonStyle.Gray
                    }
            )
        }
    }
}

@Composable
fun StaleDataNotice(
    message: String,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 6.dp
                )
                .background(
                    color =
                        LiasThemeColors.orange
                            .copy(
                                alpha = 0.10f
                            ),
                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
    ) {
        CupertinoText(
            text = "Showing last known data",
            style =
                HigTypography.subheadline,
            fontWeight =
                FontWeight.SemiBold,
            color =
                LiasThemeColors.orange
        )

        CupertinoText(
            text = message,
            style =
                HigTypography.caption,
            color =
                LiasThemeColors.secondaryLabel,
            modifier =
                Modifier.padding(
                    top = 2.dp
                )
        )

        if (
            onRefresh != null
        ) {
            HigTextButton(
                text = "Try Again",
                onClick = onRefresh
            )
        }
    }
}
