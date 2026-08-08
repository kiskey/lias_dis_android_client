// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/settings/ConnectionSettingsScreen.kt
// Version: 12.0.0
//
// Purpose:
//   Transaction-safe LIAS connection editor.
//
// Safety:
//   - Test never changes the active connection.
//   - Save always verifies before persistence.
//   - Existing saved connection survives a failed replacement.
//   - Token remains obscured on screen.
//   - Clearly explains secure credential persistence.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.ConnectionFeedback
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoScaffold
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.CupertinoTopAppBar

@Composable
fun ConnectionSettingsScreen(
    viewModel:
        SettingsViewModel,
    onBack: () -> Unit
) {

    val state by
        viewModel.uiState
            .collectAsState()

    var tempUrl by
        remember(
            state.savedServerUrl
        ) {
            mutableStateOf(
                state.savedServerUrl
            )
        }

    var tempToken by
        remember(
            state.authToken
        ) {
            mutableStateOf(
                state.authToken
            )
        }

    val hasChanges =
        tempUrl.trim() !=
            state.savedServerUrl
                .trim() ||
            tempToken !=
                state.authToken

    val busy =
        state.isTesting ||
            state.isApplyingConnection

    CupertinoScaffold(

        topBar = {

            CupertinoTopAppBar(

                title = {
                    CupertinoText(
                        "Connection"
                    )
                },

                navigationIcon = {

                    CupertinoButton(
                        onClick =
                            onBack,
                        colors =
                            CupertinoButtonDefaults
                                .plainButtonColors()
                    ) {

                        CupertinoText(
                            "‹ Settings"
                        )
                    }
                },

                actions = {

                    CupertinoButton(
                        onClick = {

                            if (
                                !hasChanges
                            ) {
                                onBack()
                                return@CupertinoButton
                            }

                            viewModel.updateServerUrl(
                                tempUrl
                            )

                            viewModel.updateAuthToken(
                                tempToken
                            )

                            viewModel.connect(
                                onSuccess =
                                    onBack
                            )
                        },
                        enabled =
                            !busy &&
                                (
                                    !hasChanges ||
                                        tempUrl
                                            .isNotBlank()
                                    ),
                        colors =
                            CupertinoButtonDefaults
                                .plainButtonColors()
                    ) {

                        CupertinoText(
                            when {

                                busy ->
                                    "Checking…"

                                hasChanges ->
                                    "Save"

                                else ->
                                    "Done"
                            }
                        )
                    }
                }
            )
        }

    ) { innerPadding ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        LiasThemeColors
                            .background
                    )
                    .padding(
                        innerPadding
                    )
                    .padding(
                        horizontal =
                            16.dp
                    )
                    .verticalScroll(
                        rememberScrollState()
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            GroupedListCard {

                Column(
                    modifier =
                        Modifier.padding(
                            16.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            14.dp
                        )
                ) {

                    HigField(
                        value =
                            tempUrl,
                        onValueChange = {
                            tempUrl =
                                it
                        },
                        label =
                            "Server URL",
                        placeholder =
                            "http://192.168.1.1:8081"
                    )

                    HigField(
                        value =
                            tempToken,
                        onValueChange = {
                            tempToken =
                                it
                        },
                        label =
                            "Authentication Token",
                        placeholder =
                            "Optional",
                        visualTransformation =
                            PasswordVisualTransformation()
                    )
                }
            }

            CupertinoText(
                text =
                    "The authentication token is stored using an Android Keystore-backed encryption key.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors
                        .secondaryLabel,
                modifier =
                    Modifier.padding(
                        horizontal =
                            4.dp
                    )
            )

            HigButton(
                text =
                    when {

                        state.isTesting ->
                            "Testing…"

                        else ->
                            "Test Connection"
                    },
                onClick = {

                    viewModel.updateServerUrl(
                        tempUrl
                    )

                    viewModel.updateAuthToken(
                        tempToken
                    )

                    viewModel.testConnection()
                },
                enabled =
                    !busy &&
                        tempUrl
                            .isNotBlank(),
                style =
                    HigButtonStyle.Secondary,
                modifier =
                    Modifier.fillMaxWidth()
            )

            ConnectionFeedback(
                message =
                    state.testResult,
                verified =
                    state.connectionVerified
            )

            if (
                hasChanges
            ) {

                CupertinoText(
                    text =
                        "Your current LIAS connection remains active until the replacement server and credentials have been verified.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors
                            .tertiaryLabel,
                    modifier =
                        Modifier.padding(
                            horizontal =
                                4.dp
                        )
                )
            }

            if (
                state.serverVersion !=
                    null
            ) {

                CupertinoText(
                    text =
                        buildString {

                            append(
                                "Last verified LIAS "
                            )

                            append(
                                state.serverVersion
                            )

                            state.healthLatencyMs
                                ?.let {
                                    append(
                                        " · "
                                    )

                                    append(
                                        it
                                    )

                                    append(
                                        " ms"
                                    )
                                }
                        },
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors
                            .secondaryLabel,
                    modifier =
                        Modifier.padding(
                            horizontal =
                                4.dp
                        )
                )
            }
        }
    }
}
