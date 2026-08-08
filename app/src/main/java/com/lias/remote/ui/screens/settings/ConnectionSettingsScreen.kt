// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/settings/ConnectionSettingsScreen.kt
// Version: 4.0.0
//
// Purpose:
//   Safely modify an existing LIAS connection.
//
// Important:
//   "Save" is now transactional.
//
//   Existing working connection:
//       remains persisted while a replacement connection is tested.
//
//   Replacement server:
//       is persisted only after /health succeeds.
//
//   This prevents a typo or temporarily unavailable server from
//   destroying an otherwise valid configuration.
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
import androidx.compose.ui.Alignment
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
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by
        viewModel.uiState.collectAsState()

    var tempUrl by
        remember(state.savedServerUrl) {
            mutableStateOf(
                state.savedServerUrl
            )
        }

    var tempToken by
        remember(state.authToken) {
            mutableStateOf(
                state.authToken
            )
        }

    val hasChanges =
        tempUrl.trim() !=
            state.savedServerUrl.trim() ||
            tempToken != state.authToken

    val canTest =
        tempUrl.isNotBlank() &&
            !state.isTesting &&
            !state.isApplyingConnection

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
                        onClick = onBack,
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
                                onSuccess = onBack
                            )
                        },

                        enabled =
                            canTest,

                        colors =
                            CupertinoButtonDefaults
                                .plainButtonColors()
                    ) {

                        CupertinoText(
                            when {

                                state.isTesting ||
                                    state.isApplyingConnection ->
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
                        LiasThemeColors.background
                    )
                    .padding(
                        innerPadding
                    )
                    .padding(
                        horizontal = 16.dp
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

                HigField(
                    value =
                        tempUrl,
                    onValueChange = {
                        tempUrl = it
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
                        tempToken = it
                    },
                    label =
                        "Auth Token (Optional)",
                    visualTransformation =
                        PasswordVisualTransformation()
                )
            }

            HigButton(
                text =
                    when {

                        state.isApplyingConnection ->
                            "Applying…"

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
                    canTest,

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
                hasChanges &&
                !state.isTesting &&
                !state.isApplyingConnection &&
                state.testResult.isNullOrBlank()
            ) {

                CupertinoText(
                    text =
                        "Your current connection stays unchanged until the new server is verified.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel,
                    modifier =
                        Modifier.align(
                            Alignment.CenterHorizontally
                        )
                )
            }

            CupertinoText(
                text =
                    "LIAS checks the server before applying a new connection.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )
        }
    }
}
