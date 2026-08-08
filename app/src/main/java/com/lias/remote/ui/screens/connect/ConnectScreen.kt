// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/connect/ConnectScreen.kt
// Version: 4.0.0
//
// Purpose:
//   First-launch LIAS connection experience.
//
// Behavioral contract:
//   - Connect never saves an unverified server.
//   - Connection is established through /health.
//   - Errors stay on the screen.
//   - No Toast is used for important connection state.
//   - The existing Cupertino visual language is preserved.
// ====================================================================

package com.lias.remote.ui.screens.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.ConnectionFeedback
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemIndigoDark
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Shield

@Composable
fun ConnectScreen(
    viewModel: SettingsViewModel,
    onConnected: () -> Unit
) {
    val state by
        viewModel.uiState.collectAsState()

    val canConnect =
        state.serverUrl.isNotBlank() &&
            !state.isTesting &&
            !state.isApplyingConnection

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    LiasThemeColors.background
                )
                .padding(
                    horizontal = 24.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Box(
            modifier =
                Modifier
                    .size(84.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape =
                            RoundedCornerShape(
                                22.dp
                            )
                    )
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        SystemBlueDark,
                                        SystemIndigoDark
                                    )
                            ),
                        shape =
                            RoundedCornerShape(
                                22.dp
                            )
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            CupertinoIcon(
                imageVector =
                    CupertinoIcons.Outlined.Shield,
                contentDescription =
                    "LIAS",
                tint =
                    Color.White,
                modifier =
                    Modifier.size(44.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        CupertinoText(
            text = "Connect to LIAS",
            style =
                HigTypography.title1,
            fontWeight =
                FontWeight.ExtraBold,
            color =
                LiasThemeColors.label,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        CupertinoText(
            text =
                "Enter your home server address to manage devices, schedules and rules.",
            style =
                HigTypography.body,
            color =
                LiasThemeColors.secondaryLabel,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        Column(
            modifier =
                Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            HigField(
                value =
                    state.serverUrl,
                onValueChange =
                    viewModel::updateServerUrl,
                label =
                    "Server URL",
                placeholder =
                    "http://192.168.1.1:8081"
            )

            HigField(
                value =
                    state.authToken,
                onValueChange =
                    viewModel::updateAuthToken,
                label =
                    "Auth Token (Optional)",
                visualTransformation =
                    PasswordVisualTransformation()
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        ConnectionFeedback(
            message =
                state.testResult,
            verified =
                state.connectionVerified
        )

        if (
            state.testResult != null
        ) {
            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )
        }

        HigButton(
            text =
                when {
                    state.isApplyingConnection ->
                        "Connecting…"

                    state.isTesting ->
                        "Checking Server…"

                    else ->
                        "Connect"
                },
            onClick = {
                viewModel.connect(
                    onSuccess = onConnected
                )
            },
            enabled =
                canConnect,
            style =
                HigButtonStyle.Primary,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        CupertinoText(
            text =
                "You can also configure this later in Settings.",
            style =
                HigTypography.caption,
            color =
                LiasThemeColors.tertiaryLabel,
            textAlign =
                TextAlign.Center
        )
    }
}
