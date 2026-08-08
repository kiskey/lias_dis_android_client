// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/SettingsViewModel.kt
// Version: 12.0.0
//
// Purpose:
//   Connection configuration, server health, appearance and advanced
//   operational controls.
//
// Important correctness rules:
//   - Test Connection never mutates the live client permanently.
//   - Save/Connect persists only after /health succeeds.
//   - Auth token is persisted before server URL so a newly selected
//     authenticated server is not first contacted with stale credentials.
//   - Real server version + measured request latency replace hard-coded
//     Settings values.
//   - Vacation state is derived from the actual global_default policy.
// ====================================================================

package com.lias.remote.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConnectionState
import com.lias.remote.core.network.ConnectionValidationError
import com.lias.remote.core.network.ConnectionValidationResult
import com.lias.remote.core.network.ConnectionValidator
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.HealthResponse
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.store.SettingsRepository
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.flushNftables
import com.lias.remote.repositories.importPolicies
import com.lias.remote.repositories.exportPolicies
import com.lias.remote.repositories.toggleVacationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(

    val serverUrl: String = "",

    val savedServerUrl: String = "",

    /**
     * In-memory editable credential.
     *
     * Persistent storage is Keystore-backed by SettingsRepository.
     */
    val authToken: String = "",

    val themeMode: String =
        "system",

    val advancedMode: Boolean =
        false,

    val vacationMode: Boolean =
        false,

    val connectionState:
        ConnectionState =
        ConnectionState.DISCONNECTED,

    val isTesting: Boolean =
        false,

    val connectionVerified: Boolean =
        false,

    val isApplyingConnection: Boolean =
        false,

    val testResult: String? =
        null,

    val hasUnsavedConnectionChanges:
        Boolean =
        false,

    val serverVersion: String? =
        null,

    val healthLatencyMs: Long? =
        null,

    val healthError: String? =
        null,

    val isRefreshingHealth: Boolean =
        false,

    val isFlushing: Boolean =
        false,

    val isExportingPolicies: Boolean =
        false,

    val isImportingPolicies: Boolean =
        false,

    val isOnboarded: Boolean =
        false
)

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

    init {

        observeSettings()
        observeRuntimeState()
    }

    private fun observeSettings() {

        viewModelScope.launch {

            settings.serverUrl
                .collect { url ->

                    val current =
                        _uiState.value

                    val formUrl =
                        if (
                            current.serverUrl
                                .isBlank()
                        ) {
                            url
                        } else {
                            current.serverUrl
                        }

                    _uiState.value =
                        current.copy(
                            serverUrl =
                                formUrl,
                            savedServerUrl =
                                url,
                            hasUnsavedConnectionChanges =
                                formUrl.trim() !=
                                    url.trim()
                        )
                }
        }

        viewModelScope.launch {

            settings.authToken
                .collect { token ->

                    /*
                     * Only replace the editable token when it has not
                     * diverged from persisted state through user input.
                     */
                    val current =
                        _uiState.value

                    if (
                        !current
                            .hasUnsavedConnectionChanges
                    ) {
                        _uiState.value =
                            current.copy(
                                authToken =
                                    token.orEmpty()
                            )
                    }
                }
        }

        viewModelScope.launch {

            settings.themeMode
                .collect { mode ->

                    _uiState.value =
                        _uiState.value.copy(
                            themeMode =
                                mode
                        )
                }
        }

        viewModelScope.launch {

            settings.isOnboarded
                .collect { onboarded ->

                    _uiState.value =
                        _uiState.value.copy(
                            isOnboarded =
                                onboarded
                        )
                }
        }

        viewModelScope.launch {

            settings.advancedMode
                .collect { enabled ->

                    _uiState.value =
                        _uiState.value.copy(
                            advancedMode =
                                enabled
                        )
                }
        }
    }

    private fun observeRuntimeState() {

        viewModelScope.launch {

            eventRepository.state
                .collect { runtime ->

                    val globalPolicy =
                        runtime.policies
                            .find {
                                it.id ==
                                    "global_default"
                            }

                    _uiState.value =
                        _uiState.value.copy(
                            connectionState =
                                runtime
                                    .connectionState,

                            vacationMode =
                                globalPolicy
                                    ?.action ==
                                    "block"
                        )
                }
        }
    }

    // ----------------------------------------------------------------
    // Form
    // ----------------------------------------------------------------

    fun updateServerUrl(
        url: String
    ) {

        val current =
            _uiState.value

        _uiState.value =
            current.copy(
                serverUrl =
                    url,
                connectionVerified =
                    false,
                testResult =
                    null,
                hasUnsavedConnectionChanges =
                    url.trim() !=
                        current
                            .savedServerUrl
                            .trim() ||
                        current.authToken
                            .isNotBlank()
            )
    }

    fun updateAuthToken(
        token: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                authToken =
                    token,
                connectionVerified =
                    false,
                testResult =
                    null,
                hasUnsavedConnectionChanges =
                    true
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

    fun setAdvancedMode(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            settings.setAdvancedMode(
                enabled
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

    // ----------------------------------------------------------------
    // Test only
    // ----------------------------------------------------------------

    fun testConnection() {

        viewModelScope.launch {

            val current =
                _uiState.value

            val validation =
                ConnectionValidator
                    .validate(
                        current.serverUrl
                    )

            if (
                validation !is
                ConnectionValidationResult.Valid
            ) {

                _uiState.value =
                    current.copy(
                        connectionVerified =
                            false,
                        testResult =
                            validationMessage(
                                validation.reason
                            )
                    )

                return@launch
            }

            val oldBaseUrl =
                api.baseUrl

            val oldToken =
                api.authToken

            _uiState.value =
                current.copy(
                    isTesting =
                        true,
                    connectionVerified =
                        false,
                    testResult =
                        null
                )

            try {

                api.baseUrl =
                    validation.normalizedUrl

                api.authToken =
                    current.authToken
                        .trim()
                        .ifBlank {
                            null
                        }

                val start =
                    SystemClock.elapsedRealtime()

                val result =
                    api.get<HealthResponse>(
                        Endpoints.HEALTH
                    )

                val latency =
                    SystemClock.elapsedRealtime() -
                        start

                when (result) {

                    is ApiResult.Success -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting =
                                    false,
                                connectionVerified =
                                    true,
                                serverVersion =
                                    result.data.version,
                                healthLatencyMs =
                                    latency,
                                healthError =
                                    null,
                                testResult =
                                    "Connection verified · LIAS ${result.data.version}"
                            )
                    }

                    else -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting =
                                    false,
                                connectionVerified =
                                    false,
                                testResult =
                                    apiErrorMessage(
                                        result
                                    )
                            )
                    }
                }

            } finally {

                /*
                 * Critical:
                 *
                 * Test Connection is side-effect free with respect to
                 * the shared live API client — even if only the token
                 * changed while the URL stayed identical.
                 */
                api.baseUrl =
                    oldBaseUrl

                api.authToken =
                    oldToken
            }
        }
    }

    // ----------------------------------------------------------------
    // Verify + persist
    // ----------------------------------------------------------------

    fun connect(
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            val current =
                _uiState.value

            val validation =
                ConnectionValidator
                    .validate(
                        current.serverUrl
                    )

            if (
                validation !is
                ConnectionValidationResult.Valid
            ) {

                _uiState.value =
                    current.copy(
                        connectionVerified =
                            false,
                        testResult =
                            validationMessage(
                                validation.reason
                            )
                    )

                return@launch
            }

            val url =
                validation.normalizedUrl

            val token =
                current.authToken
                    .trim()
                    .ifBlank {
                        null
                    }

            val oldBaseUrl =
                api.baseUrl

            val oldToken =
                api.authToken

            _uiState.value =
                current.copy(
                    isTesting =
                        true,
                    isApplyingConnection =
                        false,
                    connectionVerified =
                        false,
                    testResult =
                        null
                )

            try {

                api.baseUrl =
                    url

                api.authToken =
                    token

                val start =
                    SystemClock.elapsedRealtime()

                when (
                    val result =
                        api.get<HealthResponse>(
                            Endpoints.HEALTH
                        )
                ) {

                    is ApiResult.Success -> {

                        val latency =
                            SystemClock.elapsedRealtime() -
                                start

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting =
                                    false,
                                isApplyingConnection =
                                    true,
                                connectionVerified =
                                    true,
                                serverVersion =
                                    result.data.version,
                                healthLatencyMs =
                                    latency,
                                healthError =
                                    null,
                                testResult =
                                    "Connection verified"
                            )

                        /*
                         * Credential first.
                         *
                         * When serverUrl changes, EventRepository starts
                         * a new synchronization. The new credential should
                         * already be available before that happens.
                         */
                        settings.saveAuthToken(
                            token
                        )

                        settings.saveServerUrl(
                            url
                        )

                        _uiState.value =
                            _uiState.value.copy(
                                serverUrl =
                                    url,
                                savedServerUrl =
                                    url,
                                isApplyingConnection =
                                    false,
                                hasUnsavedConnectionChanges =
                                    false,
                                testResult =
                                    "Connected to LIAS ${result.data.version}"
                            )

                        onSuccess()
                    }

                    else -> {

                        api.baseUrl =
                            oldBaseUrl

                        api.authToken =
                            oldToken

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting =
                                    false,
                                isApplyingConnection =
                                    false,
                                connectionVerified =
                                    false,
                                testResult =
                                    apiErrorMessage(
                                        result
                                    )
                            )
                    }
                }

            } catch (
                error: Exception
            ) {

                api.baseUrl =
                    oldBaseUrl

                api.authToken =
                    oldToken

                _uiState.value =
                    _uiState.value.copy(
                        isTesting =
                            false,
                        isApplyingConnection =
                            false,
                        connectionVerified =
                            false,
                        testResult =
                            error.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to connect to LIAS."
                    )
            }
        }
    }

    fun saveSettings() {

        connect(
            onSuccess = {}
        )
    }

    // ----------------------------------------------------------------
    // Health
    // ----------------------------------------------------------------

    fun refreshServerHealth() {

        val serverUrl =
            _uiState.value
                .savedServerUrl

        if (
            serverUrl.isBlank() ||
            _uiState.value
                .isRefreshingHealth
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isRefreshingHealth =
                        true,
                    healthError =
                        null
                )

            val start =
                SystemClock.elapsedRealtime()

            when (
                val result =
                    api.get<HealthResponse>(
                        Endpoints.HEALTH
                    )
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isRefreshingHealth =
                                false,
                            serverVersion =
                                result.data.version,
                            healthLatencyMs =
                                SystemClock.elapsedRealtime() -
                                    start,
                            healthError =
                                null
                        )
                }

                else -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isRefreshingHealth =
                                false,
                            healthError =
                                apiErrorMessage(
                                    result
                                )
                        )
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Vacation Mode
    // ----------------------------------------------------------------

    fun toggleVacationMode(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .toggleVacationMode(
                            enabled
                        )
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            vacationMode =
                                result.data
                                    .vacationMode
                        )
                }

                else -> {

                    eventRepository
                        .emitUiEvent(
                            com.lias.remote.repositories.UiEvent
                                .ShowSnackbarError(
                                    apiErrorMessage(
                                        result
                                    )
                                )
                        )
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Policy backup
    // ----------------------------------------------------------------

    fun exportPolicies(
        onReady: (String) -> Unit
    ) {

        if (
            _uiState.value
                .isExportingPolicies
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isExportingPolicies =
                        true
                )

            when (
                val result =
                    eventRepository
                        .exportPolicies()
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isExportingPolicies =
                                false
                        )

                    onReady(
                        result.data
                    )
                }

                else -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isExportingPolicies =
                                false
                        )

                    eventRepository
                        .emitUiEvent(
                            com.lias.remote.repositories.UiEvent
                                .ShowSnackbarError(
                                    apiErrorMessage(
                                        result
                                    )
                                )
                        )
                }
            }
        }
    }

    fun importPolicies(
        payload: String
    ) {

        if (
            _uiState.value
                .isImportingPolicies
        ) {
            return
        }

        if (
            payload.isBlank()
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isImportingPolicies =
                        true
                )

            when (
                val result =
                    eventRepository
                        .importPolicies(
                            payload
                        )
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isImportingPolicies =
                                false
                        )

                    eventRepository
                        .emitUiEvent(
                            com.lias.remote.repositories.UiEvent
                                .ShowSnackbar(
                                    "Policies restored"
                                )
                        )
                }

                else -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isImportingPolicies =
                                false
                        )

                    eventRepository
                        .emitUiEvent(
                            com.lias.remote.repositories.UiEvent
                                .ShowSnackbarError(
                                    apiErrorMessage(
                                        result
                                    )
                                )
                        )
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Firewall maintenance
    // ----------------------------------------------------------------

    fun flushNftables() {

        if (
            _uiState.value
                .isFlushing
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isFlushing =
                        true
                )

            when (
                val result =
                    eventRepository
                        .flushNftables()
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isFlushing =
                                false
                        )
                }

                else -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isFlushing =
                                false
                        )

                    eventRepository
                        .emitUiEvent(
                            com.lias.remote.repositories.UiEvent
                                .ShowSnackbarError(
                                    apiErrorMessage(
                                        result
                                    )
                                )
                        )
                }
            }
        }
    }

    private fun validationMessage(
        error:
            ConnectionValidationError
    ): String =
        when (error) {

            ConnectionValidationError.EMPTY ->
                "Enter your LIAS server address."

            ConnectionValidationError.INVALID_FORMAT ->
                "Enter a valid server address."

            ConnectionValidationError.UNSUPPORTED_SCHEME ->
                "Use an HTTP or HTTPS server address."

            ConnectionValidationError.MISSING_HOST ->
                "The server address must include a host."

            ConnectionValidationError.INVALID_PORT ->
                "The server port must be between 1 and 65535."
        }

    private fun apiErrorMessage(
        result:
            ApiResult<*>
    ): String =
        when (result) {

            is ApiResult.Success<*> ->
                "Request completed."

            is ApiResult.AuthenticationError ->
                result.message

            is ApiResult.HttpError ->
                when (result.code) {

                    401 ->
                        "The authentication token was rejected."

                    403 ->
                        "The LIAS server denied this operation."

                    404 ->
                        "The requested LIAS endpoint was not found."

                    else ->
                        result.message
                }

            is ApiResult.ConflictError ->
                result.message

            is ApiResult.NetworkError ->
                result.cause
                    .message
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Unable to reach LIAS."

            is ApiResult.SerializationError ->
                "LIAS returned an invalid response."
        }
}
