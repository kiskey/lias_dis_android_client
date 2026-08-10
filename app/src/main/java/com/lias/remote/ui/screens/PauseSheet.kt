// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt
// Version: 24.0.0
//
// Purpose:
//   Server-aligned Pause confirmation.
//
// Backend contract:
//   POST /api/v1/devices/{pdid}/pause
//   -> fixed one-hour pause
//
// Batch 24 intentionally removes the fake 15/30/60/120-minute Pause
// selector.
//
// Compatibility:
//   onConfirm still returns Int so pre-Batch-24 call sites compile.
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
        onDismiss =
            onDismiss,
        accessibilityLabel =
            "Pause Internet"
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            24.dp,
                        vertical =
                            16.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
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
                    HigTypography.largeTitle,
                fontWeight =
                    FontWeight.Bold,
                color =
                    LiasThemeColors.red
            )

            CupertinoText(
                text =
                    "LIAS will block Internet access for this device for one hour. You can resume access early at any time.",
                style =
                    HigTypography.body,
                color =
                    LiasThemeColors.secondaryLabel,
                textAlign =
                    TextAlign.Center
            )

            CupertinoText(
                text =
                    "Infrastructure devices cannot be paused.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel,
                textAlign =
                    TextAlign.Center
            )

            HigButton(
                text =
                    "Pause for 1 Hour",
                onClick = {

                    onConfirm(
                        60
                    )
                },
                style =
                    HigButtonStyle.Danger,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}
