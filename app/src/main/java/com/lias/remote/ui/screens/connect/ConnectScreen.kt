// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/screens/connect/ConnectScreen.kt
// Version: 25.0.0
//
// Purpose:
//   First-connection experience.
//
// Batch 25:
//   - Uses Batch-22 connectAndSave() verification path.
//   - Never writes an unverified endpoint.
//   - Authentication/network/compatibility failure message comes from
//     unified SettingsViewModel diagnostics architecture.
//   - Prevents double submit.
//   - No QR action is shown unless a real QR contract exists.
// ====================================================================

package com.lias.remote.ui.screens.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigConfiguredField
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemIndigoDark
import com.slapps.cupertino.CupertinoIcon
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.Shield

@Composable
fun ConnectScreen(
    viewModel: SettingsViewModel,
    onConnected: () -> Unit
) {

    val state by
        viewModel
            .uiState
            .collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .background(
                    LiasThemeColors.background
                )
                .padding(
                    horizontal =
                        24.dp,
                    vertical =
                        24.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
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
                    )
                    .padding(
                        20.dp
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
                    null,
                tint =
                    Color.White,
                modifier =
                    Modifier
                        .padding(
                            4.dp
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
                "Enter the address of your LIAS server. The connection is verified before anything is saved.",
            style =
                HigTypography.body,
            color =
                LiasThemeColors
                    .secondaryLabel,
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

            HigConfiguredField(
                value =
                    state.serverUrl,
                onValueChange =
                    viewModel::updateServerUrl,
                label =
                    "LIAS Server",
                placeholder =
                    "http://192.168.1.1:8081",
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                enabled =
                    !state.isConnecting
            )

            HigConfiguredField(
                value =
                    state.authToken,
                onValueChange =
                    viewModel::updateAuthToken,
                label =
                    "Auth Token",
                placeholder =
                    "Optional",
                visualTransformation =
                    PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                enabled =
                    !state.isConnecting
            )
        }

        state.connectionError
            ?.let {
                message ->

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Column(
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
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {

                    CupertinoText(
                        text =
                            "Couldn’t Connect",
                        style =
                            HigTypography.headline,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            LiasThemeColors.red
                    )

                    CupertinoText(
                        text =
                            message,
                        style =
                            HigTypography.subheadline,
                        color =
                            LiasThemeColors
                                .secondaryLabel
                    )
                }
            }

        Spacer(
            modifier =
                Modifier.height(
                    22.dp
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

                viewModel.connectAndSave(
                    onConnected =
                        onConnected
                )
            },
            enabled =
                !state.isConnecting &&
                    state.serverUrl
                        .isNotBlank(),
            style =
                HigButtonStyle.Primary,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        CupertinoText(
            text =
                "Your authentication token stays in this app’s local settings and is not included in diagnostics.",
            style =
                HigTypography.caption,
            color =
                LiasThemeColors
                    .tertiaryLabel,
            textAlign =
                TextAlign.Center
        )
    }
}
