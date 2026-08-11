// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/settings/ConnectionSettingsScreen.kt
// Version: 22.0.0
//
// Purpose:
//   Safe connection editing + advanced diagnostics.
//
// UX tiers:
//
//   Normal:
//     LIAS Server
//     Auth Token
//     Test
//     Save
//
//   Advanced:
//     live SSE state
//     sanitized endpoint
//     recent diagnostic events
//
// Technical detail is deliberately collapsed by default.
// ====================================================================

package com.lias.remote.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lias.remote.core.diagnostics.ErrorPresentation
import com.lias.remote.core.network.ConnectionState
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigConfiguredField
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoScaffold
import com.slapps.cupertino.CupertinoNavigateBackButton
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.CupertinoTopAppBar

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun ConnectionSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {

    val state by
        viewModel.uiState
            .collectAsState()

    var showDiagnostics by
        remember {
            mutableStateOf(
                false
            )
        }

    CupertinoScaffold(
        topBar = {

            CupertinoTopAppBar(
                title = {

                    CupertinoText(
                        "Connection",
            color = LiasThemeColors.label
                    )
                },
                navigationIcon = {

                    CupertinoNavigateBackButton(
                        onClick = {

                            if (
                                state.hasConnectionDraftChanges
                            ) {
                                viewModel
                                    .revertConnectionDraft()
                            }

                            onBack()
                        },
                        title = {
                            CupertinoText(
                                text =
                                    "Settings"
                            )
                        }
                    )
                },
                actions = {

                    HigTextButton(
                        text =
                            when {

                                state.isSavingConnection ->
                                    "Saving…"

                                else ->
                                    "Save"
                            },
                        onClick = {

                            viewModel
                                .saveSettings(
                                    onSaved =
                                        onBack
                                )
                        },
                        enabled =
                            !state.isSavingConnection &&
                                !state.isTesting
                    )
                }
            )
        }
    ) {
        innerPadding ->

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
                    .imePadding()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal =
                            16.dp,
                        vertical =
                            16.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            GroupedListCard {

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
                        )
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
                        )
                )
            }

            CupertinoText(
                text =
                    "Changes are verified against LIAS before replacing the current working connection.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.secondaryLabel
            )

            HigButton(
                text =
                    if (
                        state.isTesting
                    ) {
                        "Testing…"
                    } else {
                        "Test Connection"
                    },
                onClick =
                    viewModel::testConnection,
                enabled =
                    !state.isTesting &&
                        !state.isSavingConnection,
                style =
                    HigButtonStyle.Secondary,
                modifier =
                    Modifier.fillMaxWidth()
            )

            state.testResult
                ?.let {
                    result ->

                    CupertinoText(
                        text =
                            result,
                        style =
                            HigTypography.subheadline,
                        color =
                            LiasThemeColors.green
                    )
                }

            state.connectionError
                ?.let {
                    error ->

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    LiasThemeColors.red
                                        .copy(
                                            alpha =
                                                0.10f
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
                                "Unable to Use This Connection",
                            style =
                                HigTypography.headline,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                LiasThemeColors.red
                        )

                        CupertinoText(
                            text =
                                error,
                            style =
                                HigTypography.subheadline,
                            color =
                                LiasThemeColors.secondaryLabel
                        )
                    }
                }

            CupertinoText(
                text =
                    "CURRENT CONNECTION",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            GroupedListCard {

                GroupedListRow(
                    primaryText =
                        "Server",
                    secondaryText =
                        ErrorPresentation
                            .safeEndpoint(
                                state.savedServerUrl
                            ),
                    showDivider =
                        true
                )

                GroupedListRow(
                    primaryText =
                        "Live Updates",
                    secondaryText =
                        connectionStateDescription(
                            state.connectionState
                        ),
                    trailingContent = {

                        StatusPill(
                            text =
                                connectionStateTitle(
                                    state.connectionState
                                ),
                            tone =
                                when (
                                    state.connectionState
                                ) {

                                    ConnectionState.CONNECTED ->
                                        PillTone.ALLOWED

                                    ConnectionState.CONNECTING,
                                    ConnectionState.RECONNECTING ->
                                        PillTone.SCHEDULED

                                    ConnectionState.DISCONNECTED ->
                                        PillTone.WARN
                                }
                        )
                    }
                )
            }

            HigButton(
                text =
                    if (
                        showDiagnostics
                    ) {
                        "Hide Diagnostics"
                    } else {
                        "Show Diagnostics"
                    },
                onClick = {

                    showDiagnostics =
                        !showDiagnostics
                },
                style =
                    HigButtonStyle.Gray,
                modifier =
                    Modifier.fillMaxWidth()
            )

            if (
                showDiagnostics
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        CupertinoText(
                            text =
                                "Advanced Diagnostics",
                            style =
                                HigTypography.headline,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                LiasThemeColors.label
                        )

                        CupertinoText(
                            text =
                                "Authentication tokens are never included.",
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors.tertiaryLabel
                        )
                    }

                    if (
                        state.diagnostics
                            .isNotEmpty()
                    ) {

                        HigTextButton(
                            text =
                                "Clear",
                            onClick =
                                viewModel::clearDiagnostics
                        )
                    }
                }

                if (
                    state.diagnostics
                        .isEmpty()
                ) {

                    GroupedListCard {

                        GroupedListRow(
                            primaryText =
                                "No diagnostics recorded",
                            secondaryText =
                                "Connection and test events will appear here."
                        )
                    }

                } else {

                    GroupedListCard {

                        state.diagnostics
                            .forEachIndexed {
                                    index,
                                    record ->

                                GroupedListRow(
                                    primaryText =
                                        record.title,
                                    secondaryText =
                                        buildString {

                                            append(
                                                record.summary
                                            )

                                            record
                                                .technicalDetail
                                                ?.takeIf {
                                                    it.isNotBlank()
                                                }
                                                ?.let {

                                                    append(
                                                        "\n"
                                                    )

                                                    append(
                                                        it
                                                    )
                                                }

                                            append(
                                                "\n"
                                            )

                                            append(
                                                record.timestamp
                                            )
                                        },
                                    showDivider =
                                        index <
                                            state.diagnostics
                                                .lastIndex
                                )
                            }
                    }
                }
            }
        }
    }
}

private fun connectionStateTitle(
    state: ConnectionState
): String =
    when (
        state
    ) {

        ConnectionState.CONNECTED ->
            "Connected"

        ConnectionState.CONNECTING ->
            "Connecting"

        ConnectionState.RECONNECTING ->
            "Reconnecting"

        ConnectionState.DISCONNECTED ->
            "Offline"
    }

private fun connectionStateDescription(
    state: ConnectionState
): String =
    when (
        state
    ) {

        ConnectionState.CONNECTED ->
            "Real-time LIAS events are active."

        ConnectionState.CONNECTING ->
            "Opening the real-time event stream."

        ConnectionState.RECONNECTING ->
            "The event stream was interrupted and is recovering."

        ConnectionState.DISCONNECTED ->
            "No real-time LIAS event stream is currently active."
    }
