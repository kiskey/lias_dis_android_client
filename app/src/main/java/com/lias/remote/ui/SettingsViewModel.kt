// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/SettingsViewModel.kt
// Version: 20.0.0
//
// Purpose:
//   Connection/settings state owner.
//
// Batch 20:
//   - Distinguishes "settings still loading" from "not configured".
//   - Hydrates persisted connection values as one logical snapshot.
//   - First-launch Connect verifies LIAS before persisting credentials.
//   - Failed connection attempts do not replace saved configuration.
//   - Retains existing settings/vacation/nftables interfaces.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.HealthResponse
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.store.SettingsRepository
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.flushNftables
import com.lias.remote.repositories.toggleVacationMode
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(

    /*
     * Editable draft values.
     */
    val serverUrl: String = "",
    val authToken: String = "",

    /*
     * Persisted authoritative configuration.
     */
    val savedServerUrl: String = "",
    val savedAuthToken: String = "",

    /*
     * False until DataStore has produced its initial persisted snapshot.
     *
     * LiasNavHost MUST NOT decide whether ConnectScreen is required
     * until this becomes true.
     */
    val isConfigurationLoaded: Boolean = false,

    val themeMode: String = "system",
    val vacationMode: Boolean = false,
    val isOnboarded: Boolean = false,

    val isTesting: Boolean = false,
    val isConnecting: Boolean = false,

    val testResult: String? = null,
    val connectionError: String? = null,

    val isFlushing: Boolean = false
) {

    val isConfigured: Boolean
        get() =
            savedServerUrl
                .isNotBlank()
}

class SettingsViewModel(
    private val settings:
        SettingsRepository,
    private val api:
        LiasApiClient,
    private val eventRepository:
        EventRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            SettingsUiState()
        )

    val uiState:
        StateFlow<SettingsUiState> =
        _uiState.asStateFlow()

    /*
     * Prevent later DataStore emissions from overwriting text the user
     * is actively editing in Connection Settings.
     */
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
     * First-launch connection path.
     *
     * Unlike the old Connect button, this does NOT persist a server
     * merely because the URL field is non-empty.
     *
     * The health endpoint must respond successfully first.
     */
    fun connectAndSave(
        onConnected: () -> Unit
    ) {

        if (
            _uiState.value.isConnecting
        ) {
            return
        }

        val candidateUrl =
            normalizeServerUrl(
                _uiState.value.serverUrl
            )

        val candidateToken =
            _uiState.value.authToken
                .trim()

        val validationError =
            validateServerUrl(
                candidateUrl
            )

        if (
            validationError !=
            null
        ) {

            _uiState.value =
                _uiState.value.copy(
                    connectionError =
                        validationError,
                    testResult =
                        null
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    serverUrl =
                        candidateUrl,
                    isConnecting =
                        true,
                    connectionError =
                        null,
                    testResult =
                        null
                )

            val previousApiUrl =
                api.baseUrl

            val previousApiToken =
                api.authToken

            /*
             * LiasApiClient is the currently supplied probe surface.
             * Do not persist the candidate until the health check passes.
             */
            api.baseUrl =
                candidateUrl

            api.authToken =
                candidateToken
                    .ifBlank {
                        null
                    }

            val result =
                api.get<HealthResponse>(
                    Endpoints.HEALTH
                )

            when (
                result
            ) {

                is ApiResult.Success -> {

                    settings.saveServerUrl(
                        candidateUrl
                    )

                    settings.saveAuthToken(
                        candidateToken
                            .ifBlank {
                                null
                            }
                    )

                    _uiState.value =
                        _uiState.value.copy(
                            savedServerUrl =
                                candidateUrl,
                            savedAuthToken =
                                candidateToken,
                            isConnecting =
                                false,
                            connectionError =
                                null,
                            testResult =
                                "Connected to LIAS ${result.data.version}"
                        )

                    onConnected()
                }

                else -> {

                    /*
                     * Candidate was only a probe.
                     *
                     * Restore the currently persisted endpoint so a
                     * failed edit cannot leave the shared API client
                     * pointing at the rejected server.
                     */
                    api.baseUrl =
                        previousApiUrl

                    api.authToken =
                        previousApiToken

                    _uiState.value =
                        _uiState.value.copy(
                            isConnecting =
                                false,
                            connectionError =
                                connectionFailureMessage(
                                    result
                                )
                        )
                }
            }
        }
    }

    /**
     * Non-persisting connection test used by Connection Settings.
     */
    fun testConnection() {

        if (
            _uiState.value.isTesting ||
            _uiState.value.isConnecting
        ) {
            return
        }

        val candidateUrl =
            normalizeServerUrl(
                _uiState.value.serverUrl
            )

        val validationError =
            validateServerUrl(
                candidateUrl
            )

        if (
            validationError !=
            null
        ) {

            _uiState.value =
                _uiState.value.copy(
                    connectionError =
                        validationError,
                    testResult =
                        null
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isTesting =
                        true,
                    connectionError =
                        null,
                    testResult =
                        null
                )

            val previousApiUrl =
                api.baseUrl

            val previousApiToken =
                api.authToken

            api.baseUrl =
                candidateUrl

            api.authToken =
                _uiState.value
                    .authToken
                    .trim()
                    .ifBlank {
                        null
                    }

            val result =
                api.get<HealthResponse>(
                    Endpoints.HEALTH
                )

            api.baseUrl =
                previousApiUrl

            api.authToken =
                previousApiToken

            _uiState.value =
                _uiState.value.copy(
                    isTesting =
                        false,
                    testResult =
                        when (
                            result
                        ) {

                            is ApiResult.Success ->
                                "Connection successful · LIAS ${result.data.version}"

                            else ->
                                null
                        },
                    connectionError =
                        when (
                            result
                        ) {

                            is ApiResult.Success ->
                                null

                            else ->
                                connectionFailureMessage(
                                    result
                                )
                        }
                )
        }
    }

    /**
     * Existing Connection Settings save interface.
     *
     * This remains explicit because an already-configured user may
     * intentionally replace a server after testing it.
     */
    fun saveSettings() {

        val candidateUrl =
            normalizeServerUrl(
                _uiState.value.serverUrl
            )

        val validationError =
            validateServerUrl(
                candidateUrl
            )

        if (
            validationError !=
            null
        ) {

            _uiState.value =
                _uiState.value.copy(
                    connectionError =
                        validationError
                )

            return
        }

        viewModelScope.launch {

            val token =
                _uiState.value
                    .authToken
                    .trim()

            settings.saveServerUrl(
                candidateUrl
            )

            settings.saveAuthToken(
                token.ifBlank {
                    null
                }
            )

            _uiState.value =
                _uiState.value.copy(
                    serverUrl =
                        candidateUrl,
                    savedServerUrl =
                        candidateUrl,
                    savedAuthToken =
                        token,
                    connectionError =
                        null,
                    testResult =
                        "Settings saved."
                )
        }
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

            if (
                result is
                ApiResult.Success
            ) {

                _uiState.value =
                    _uiState.value.copy(
                        vacationMode =
                            result.data
                                .vacationMode
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

            eventRepository
                .flushNftables()

            _uiState.value =
                _uiState.value.copy(
                    isFlushing =
                        false
                )
        }
    }

    private fun normalizeServerUrl(
        raw: String
    ): String {

        val trimmed =
            raw.trim()
                .trimEnd(
                    '/'
                )

        if (
            trimmed.isBlank()
        ) {
            return ""
        }

        return if (
            trimmed.startsWith(
                "http://",
                ignoreCase = true
            ) ||
            trimmed.startsWith(
                "https://",
                ignoreCase = true
            )
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }

    private fun validateServerUrl(
        url: String
    ): String? {

        if (
            url.isBlank()
        ) {
            return "Enter the LIAS server address."
        }

        return try {

            val parsed =
                URI(
                    url
                )

            when {

                parsed.scheme !in
                    setOf(
                        "http",
                        "https"
                    ) ->
                    "LIAS Server must use http:// or https://."

                parsed.host
                    .isNullOrBlank() ->
                    "Enter a valid LIAS server address."

                else ->
                    null
            }

        } catch (
            _: Exception
        ) {
            "Enter a valid LIAS server address."
        }
    }

    private fun connectionFailureMessage(
        result: ApiResult<*>
    ): String =
        when (
            result
        ) {

            is ApiResult.HttpError ->

                when (
                    result.code
                ) {

                    401,
                    403 ->
                        "Authentication failed. Check the LIAS Auth Token."

                    404 ->
                        "A server responded, but the LIAS health endpoint was not found."

                    else ->
                        result.message
                            .ifBlank {
                                "LIAS returned HTTP ${result.code}."
                            }
                }

            is ApiResult.NetworkError ->

                result.cause
                    .message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "LIAS could not be reached."

            is ApiResult.ConflictError ->
                "LIAS rejected the request."

            else ->
                "Unable to connect to LIAS."
        }

    private data class PersistedSettings(
        val serverUrl: String,
        val authToken: String,
        val themeMode: String,
        val onboarded: Boolean
    )
}
