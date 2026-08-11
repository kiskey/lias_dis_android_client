// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt
// Version: 35.5.1
//
// Purpose:
//   Server-aligned one-hour Pause confirmation.
//
// Plan 3.5 refinement:
//   - Compact Cupertino sheet: Medium first, Large remains available.
//   - Focused copy keeps the destructive action visible at Medium.
//   - Fixed one-hour LIAS server contract is unchanged.
//
// Backend contract:
//   POST /api/v1/devices/{pdid}/pause
//   -> fixed one-hour pause.
//
// Compatibility:
//   onConfirm still returns Int.
//   The only value this sheet ever returns is 60.
// ====================================================================

package com.lias.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigSheetPresentation
import com.lias.remote.ui.components.rememberHigAnimatedCompletion
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

@Composable
fun PauseSheet(
    targetLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit
) {
    HigModalSheet(
        presentation =
            HigSheetPresentation.Compact,
        onDismiss =
            onDismiss,
        accessibilityLabel =
            "Pause Internet"
    ) {
        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            24.dp,
                        vertical =
                            12.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            HigSheetHeader(
                title =
                    "Pause Internet",
                onCancel =
                    onDismiss
            )

            CupertinoText(
                text =
                    targetLabel,
                style =
                    HigTypography.title2,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    LiasThemeColors.label,
                textAlign =
                    TextAlign.Center
            )

            CupertinoText(
                text =
                    "1 Hour",
                style =
                    HigTypography.title1,
                fontWeight =
                    FontWeight.Bold,
                color =
                    LiasThemeColors.red
            )

            CupertinoText(
                text =
                    "Pauses internet for 1 hour. You can resume access early at any time.",
                style =
                    HigTypography.body,
                color =
                    LiasThemeColors.secondaryLabel,
                textAlign =
                    TextAlign.Center
            )

            HigButton(
                text =
                    "Pause for 1 Hour",
                onClick = {
                    animatedComplete {
                        onConfirm(
                            60
                        )
                    }
                },
                style =
                    HigButtonStyle.Danger,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}
