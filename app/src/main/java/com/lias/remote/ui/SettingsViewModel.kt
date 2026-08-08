// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/SettingsViewModel.kt
// Version: 22.0.0
//
// Purpose:
//   Settings + safe server replacement + diagnostics state.
//
// Batch 22:
//   - Uses isolated LiasConnectionProbe.
//   - No candidate probe mutates live EventRepository REST state.
//   - Server replacement requires a successful probe.
//   - Authentication/network/serialization failures are distinguished.
//   - Technical diagnostics are retained separately from normal UI.
//   - Credentials are never included in diagnostics.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.diagnostics.DiagnosticRecord
import com.lias.remote.core.diagnostics.ErrorPresentation
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConnectionState
import com.lias.remote.core.network.LiasConnectionProbe
import com.lias.remote.core.store.SettingsRepository
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.flushNftables
import com.lias.remote.repositories.toggleVacationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val authToken: String = "",

    val savedServerUrl: String = "",
    val savedAuthToken: String = "",

    val isConfigurationLoaded: Boolean = false,

    val themeMode: String = "system",
    val vacationMode: Boolean = false,
    val isOnboarded: Boolean = false,

    val isTesting: Boolean = false,
    val isConnecting: Boolean = false,
    val isSavingConnection: Boolean = false,

    val testResult: String? = null,
    val connectionError: String? = null,

    val isFlushing: Boolean = false,

    val diagnostics: List<DiagnosticRecord> =
        emptyList(),

    val connectionState:
        ConnectionState =
        ConnectionState.DISCONNECTED
) {

    val isConfigured: Boolean
        get() =
            savedServerUrl
                .isNotBlank()

    val hasConnectionDraftChanges: Boolean
        get() =
            serverUrl
                .trim()
                .trimEnd('/') !=
            savedServerUrl
                .trim()
                .trimEnd('/') ||
                authToken.trim() !=
                savedAuthToken.trim()
}

class SettingsViewModel(
    private val settings:
        SettingsRepository,
    private val connectionProbe:
        LiasConnectionProbe,
    private val eventRepository:
        EventRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            SettingsUiState()
        )

    val uiState:
        StateFlow<SettingsUiState> =
        _uiState
            .asStateFlow()

    private var draftHydrated =
        false

    init {

        viewModelScope.launch {

            combine(
                settings.serverUrl,
                settings.authToken,
                settings.themeMode,
                settings.isOnboarded
            ) {
                    serverUrl,
                    authToken,
                    themeMode,
                    onboarded ->

                PersistedSettings(
                    serverUrl =
                        serverUrl,
                    authToken =
                        authToken.orEmpty(),
                    themeMode =
                        themeMode,
                    onboarded =
                        onboarded
                )
            }
                .collect {
                    persisted ->

                    val current =
                        _uiState.value

                    val hydrateDraft =
                        !draftHydrated

                    if (
                        hydrateDraft
                    ) {
                        draftHydrated =
                            true
                    }

                    _uiState.value =
                        current.copy(
                            serverUrl =
                                if (
                                    hydrateDraft
                                ) {
                                    persisted.serverUrl
                                } else {
                                    current.serverUrl
                                },
                            authToken =
                                if (
                                    hydrateDraft
                                ) {
                                    persisted.authToken
                                } else {
                                    current.authToken
                                },
                            savedServerUrl =
                                persisted.serverUrl,
                            savedAuthToken =
                                persisted.authToken,
                            themeMode =
                                persisted.themeMode,
                            isOnboarded =
                                persisted.onboarded,
                            isConfigurationLoaded =
                                true
                        )
                }
        }

        viewModelScope.launch {

            eventRepository.state
                .collect {
                    repositoryState ->

                    val previous =
                        _uiState.value
                            .connectionState

                    val next =
                        repositoryState
                            .connectionState

                    _uiState.value =
                        _uiState.value.copy(
                            connectionState =
                                next
                        )

                    if (
                        previous !=
                        next
                    ) {

                        appendDiagnostic(
                            ErrorPresentation
                                .connectionDiagnostic(
                                    next
                                )
                        )
                    }
                }
        }
    }

    fun updateServerUrl(
        url: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                serverUrl =
                    url,
                testResult =
                    null,
                connectionError =
                    null
            )
    }

    fun updateAuthToken(
        token: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                authToken =
                    token,
                testResult =
                    null,
                connectionError =
                    null
            )
    }

    fun updateThemeMode(
        mode: String
    ) {

        viewModelScope.launch {

            settings.saveThemeMode(
                mode
            )
        }
    }

    fun completeOnboarding() {

        viewModelScope.launch {

            settings.setOnboarded(
                true
            )
        }
    }

    /**
     * Initial connection.
     *
     * Persist only after verified LIAS health.
     */
    fun connectAndSave(
        onConnected: () -> Unit
    ) {

        if (
            _uiState.value
                .isConnecting
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isConnecting =
                        true,
                    connectionError =
                        null,
                    testResult =
                        null
                )

            val result =
                connectionProbe.probe(
                    rawUrl =
                        _uiState.value
                            .serverUrl,
                    authToken =
                        _uiState.value
                            .authToken
                )

            when (
                result
            ) {

                is ApiResult.Success -> {

                    persistVerifiedConnection(
                        url =
                            result.data
                                .normalizedUrl,
                        token =
                            _uiState.value
                                .authToken
                    )

                    _uiState.value =
                        _uiState.value.copy(
                            isConnecting =
                                false,
                            testResult =
                                "Connected to LIAS ${result.data.health.version}",
                            connectionError =
                                null
                        )

                    appendDiagnostic(
                        DiagnosticRecord(
                            timestamp =
                                java.time.Instant
                                    .now()
                                    .toString(),
                            kind =
                                com.lias.remote
                                    .core
                                    .diagnostics
                                    .DiagnosticKind
                                    .INFORMATION,
                            title =
                                "Connection Verified",
                            summary =
                                "LIAS health check succeeded.",
                            technicalDetail =
                                ErrorPresentation
                                    .safeEndpoint(
                                        result.data
                                            .normalizedUrl
                                    )
                        )
                    )

                    onConnected()
                }

                else -> {

                    presentProbeFailure(
                        result
                    )

                    _uiState.value =
                        _uiState.value.copy(
                            isConnecting =
                                false
                        )
                }
            }
        }
    }

    /**
     * Candidate test only.
     *
     * Does NOT persist or alter active repository configuration.
     */
    fun testConnection() {

        if (
            _uiState.value
                .isTesting
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isTesting =
                        true,
                    testResult =
                        null,
                    connectionError =
                        null
                )

            val result =
                connectionProbe.probe(
                    rawUrl =
                        _uiState.value
                            .serverUrl,
                    authToken =
                        _uiState.value
                            .authToken
                )

            when (
                result
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isTesting =
                                false,
                            serverUrl =
                                result.data
                                    .normalizedUrl,
                            testResult =
                                "Connection successful · LIAS ${result.data.health.version}",
                            connectionError =
                                null
                        )

                    appendDiagnostic(
                        DiagnosticRecord(
                            timestamp =
                                java.time.Instant
                                    .now()
                                    .toString(),
                            kind =
                                com.lias.remote
                                    .core
                                    .diagnostics
                                    .DiagnosticKind
                                    .INFORMATION,
                            title =
                                "Connection Test Passed",
                            summary =
                                "Candidate LIAS server responded successfully.",
                            technicalDetail =
                                ErrorPresentation
                                    .safeEndpoint(
                                        result.data
                                            .normalizedUrl
                                    )
                        )
                    )
                }

                else -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isTesting =
                                false
                        )

                    presentProbeFailure(
                        result
                    )
                }
            }
        }
    }

    /**
     * Server replacement is verification-gated.
     *
     * Unlike the older implementation, Save cannot persist an
     * unreachable/mistyped replacement endpoint.
     */
    fun saveSettings(
        onSaved: (() -> Unit)? = null
    ) {

        if (
            _uiState.value
                .isSavingConnection
        ) {
            return
        }

        if (
            !_uiState.value
                .hasConnectionDraftChanges
        ) {

            onSaved
                ?.invoke()

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isSavingConnection =
                        true,
                    connectionError =
                        null,
                    testResult =
                        null
                )

            val result =
                connectionProbe.probe(
                    rawUrl =
                        _uiState.value
                            .serverUrl,
                    authToken =
                        _uiState.value
                            .authToken
                )

            when (
                result
            ) {

                is ApiResult.Success -> {

                    persistVerifiedConnection(
                        url =
                            result.data
                                .normalizedUrl,
                        token =
                            _uiState.value
                                .authToken
                    )

                    _uiState.value =
                        _uiState.value.copy(
                            isSavingConnection =
                                false,
                            testResult =
                                "Connection settings saved."
                        )

                    onSaved
                        ?.invoke()
                }

                else -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSavingConnection =
                                false
                        )

                    presentProbeFailure(
                        result
                    )
                }
            }
        }
    }

    fun revertConnectionDraft() {

        _uiState.value =
            _uiState.value.copy(
                serverUrl =
                    _uiState.value
                        .savedServerUrl,
                authToken =
                    _uiState.value
                        .savedAuthToken,
                connectionError =
                    null,
                testResult =
                    null
            )
    }

    fun clearDiagnostics() {

        _uiState.value =
            _uiState.value.copy(
                diagnostics =
                    emptyList()
            )
    }

    fun toggleVacationMode(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            val result =
                eventRepository
                    .toggleVacationMode(
                        enabled
                    )

            when (
                result
            ) {

                is ApiResult.Success ->

                    _uiState.value =
                        _uiState.value.copy(
                            vacationMode =
                                result.data
                                    .vacationMode
                        )

                else ->

                    appendDiagnostic(
                        ErrorPresentation
                            .diagnostic(
                                result,
                                _uiState.value
                                    .savedServerUrl
                            )
                    )
            }
        }
    }

    fun flushNftables() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isFlushing =
                        true
                )

            val result =
                eventRepository
                    .flushNftables()

            if (
                result !is
                ApiResult.Success
            ) {

                appendDiagnostic(
                    ErrorPresentation
                        .diagnostic(
                            result,
                            _uiState.value
                                .savedServerUrl
                        )
                )
            }

            _uiState.value =
                _uiState.value.copy(
                    isFlushing =
                        false
                )
        }
    }

    private suspend fun persistVerifiedConnection(
        url: String,
        token: String
    ) {

        val normalizedToken =
            token.trim()

        settings.saveServerUrl(
            url
        )

        settings.saveAuthToken(
            normalizedToken
                .ifBlank {
                    null
                }
        )

        _uiState.value =
            _uiState.value.copy(
                serverUrl =
                    url,
                savedServerUrl =
                    url,
                savedAuthToken =
                    normalizedToken,
                connectionError =
                    null
            )
    }

    private fun presentProbeFailure(
        result: ApiResult<*>
    ) {

        val presentation =
            ErrorPresentation
                .from(
                    result
                )

        _uiState.value =
            _uiState.value.copy(
                testResult =
                    null,
                connectionError =
                    presentation.message
            )

        appendDiagnostic(
            ErrorPresentation
                .diagnostic(
                    result,
                    _uiState.value
                        .serverUrl
                )
        )
    }

    private fun appendDiagnostic(
        record: DiagnosticRecord
    ) {

        _uiState.value =
            _uiState.value.copy(
                diagnostics =
                    (
                        listOf(
                            record
                        ) +
                            _uiState.value
                                .diagnostics
                        )
                        .take(
                            MAX_DIAGNOSTICS
                        )
            )
    }

    private data class PersistedSettings(
        val serverUrl: String,
        val authToken: String,
        val themeMode: String,
        val onboarded: Boolean
    )

    companion object {

        private const val MAX_DIAGNOSTICS =
            30
    }
}
