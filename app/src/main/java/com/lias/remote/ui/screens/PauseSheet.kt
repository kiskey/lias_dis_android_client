// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt
// Version: 10.0.0
//
// Purpose:
//   Confirmation UI for LIAS Pause Internet.
//
// Critical correction:
//   LIAS pause duration is NOT user-configurable.
//
// Backend:
//       POST /api/v1/devices/{pdid}/pause
//
// always creates a one-hour pause.
//
// The callback retains its existing (minutes: Int) signature so older
// callers continue to compile, but this sheet can emit only 60.
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
import com.lias.remote.core.network.FIXED_PAUSE_MINUTES
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun PauseSheet(
    targetLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit
) {
    HigModalSheet(
        onDismiss =
            onDismiss
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
                    HigTypography.title1,
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    LiasThemeColors.red
            )

            CupertinoText(
                text =
                    "LIAS will block internet access for this device for one hour. You can resume access earlier at any time.",
                style =
                    HigTypography.subheadline,
                color =
                    LiasThemeColors.secondaryLabel,
                textAlign =
                    TextAlign.Center
            )

            CupertinoText(
                text =
                    "The pause timer is maintained by the LIAS server, so it continues even if this app is closed.",
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
                        FIXED_PAUSE_MINUTES
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
