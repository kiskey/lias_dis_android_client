// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/connect/ConnectScreen.kt
// Version: 20.0.0
//
// Purpose:
//   First-run LIAS connection gate.
//
// Batch 20:
//   - Connect means "verify AND persist", not merely "save URL".
//   - Wrong token stays on Connect screen.
//   - Unreachable server stays on Connect screen.
//   - User-visible error remains actionable.
//   - No fake QR scanner action.
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
        viewModel.uiState
            .collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    LiasThemeColors.background
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        84.dp
                    )
                    .shadow(
                        elevation =
                            12.dp,
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
                    CupertinoIcons
                        .Outlined
                        .Shield,
                contentDescription =
                    "LIAS",
                tint =
                    Color.White,
                modifier =
                    Modifier.size(
                        44.dp
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )

        CupertinoText(
            text =
                "Connect to LIAS",
            style =
                HigTypography.title1,
            fontWeight =
                FontWeight.Bold,
            color =
                LiasThemeColors.label,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        CupertinoText(
            text =
                "Enter the address and authentication token for your LIAS server.",
            style =
                HigTypography.body,
            color =
                LiasThemeColors.secondaryLabel,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(
                    28.dp
                )
        )

        Column(
            modifier =
                Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            HigField(
                value =
                    state.serverUrl,
                onValueChange =
                    viewModel::updateServerUrl,
                label =
                    "LIAS Server",
                placeholder =
                    "http://192.168.1.1:8081"
            )

            HigField(
                value =
                    state.authToken,
                onValueChange =
                    viewModel::updateAuthToken,
                label =
                    "Auth Token",
                placeholder =
                    "Optional if LIAS authentication is disabled",
                visualTransformation =
                    PasswordVisualTransformation()
            )
        }

        state.connectionError
            ?.let {
                error ->

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color =
                                    LiasThemeColors.red
                                        .copy(
                                            alpha =
                                                0.10f
                                        ),
                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    )
                            )
                            .padding(
                                12.dp
                            )
                ) {

                    CupertinoText(
                        text =
                            error,
                        style =
                            HigTypography.subheadline,
                        color =
                            LiasThemeColors.red
                    )
                }
            }

        state.testResult
            ?.let {
                message ->

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                CupertinoText(
                    text =
                        message,
                    style =
                        HigTypography.subheadline,
                    color =
                        LiasThemeColors.green,
                    textAlign =
                        TextAlign.Center
                )
            }

        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )

        HigButton(
            text =
                if (
                    state.isConnecting
                ) {
                    "Connecting…"
                } else {
                    "Connect"
                },
            onClick = {

                viewModel
                    .connectAndSave(
                        onConnected =
                            onConnected
                    )
            },
            enabled =
                state.serverUrl
                    .isNotBlank() &&
                    !state.isConnecting,
            style =
                HigButtonStyle.Primary,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        CupertinoText(
            text =
                "Your token is stored locally with the app configuration and is never included in LIAS navigation links.",
            style =
                HigTypography.caption,
            color =
                LiasThemeColors.tertiaryLabel,
            textAlign =
                TextAlign.Center
        )
    }
}
