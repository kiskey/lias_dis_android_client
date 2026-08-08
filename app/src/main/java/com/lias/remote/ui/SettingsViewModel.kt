// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/SettingsViewModel.kt
// Version: 4.0.0
//
// Purpose:
//   Own all connection configuration state and make connection changes
//   transactional.
//
// Critical behavior:
//   A new server URL is NOT persisted merely because the user pressed
//   Connect or Save.
//
//   The new configuration must first successfully answer /health.
//
//   Only after successful verification:
//       1. server URL is persisted
//       2. auth token is persisted
//       3. EventRepository observes the DataStore change
//       4. normal SSE/data synchronization can begin
//
//   This avoids the previous false-positive "connected" state.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConnectionValidator
import com.lias.remote.core.network.ConnectionValidationError
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.HealthResponse
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.store.SettingsRepository
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.flushNftables
import com.lias.remote.repositories.toggleVacationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val savedServerUrl: String = "",
    val authToken: String = "",
    val themeMode: String = "system",
    val vacationMode: Boolean = false,

    val isTesting: Boolean = false,

    /**
     * True only when the current input has passed /health.
     */
    val connectionVerified: Boolean = false,

    /**
     * True while the verified configuration is being persisted.
     */
    val isApplyingConnection: Boolean = false,

    val testResult: String? = null,

    /**
     * True when the current form differs from the persisted configuration.
     */
    val hasUnsavedConnectionChanges: Boolean = false,

    val isFlushing: Boolean = false,

    val isOnboarded: Boolean = true
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val api: LiasApiClient,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            SettingsUiState()
        )

    val uiState: StateFlow<SettingsUiState> =
        _uiState.asStateFlow()

    init {

        viewModelScope.launch {
            settings.serverUrl.collect { url ->

                val current =
                    _uiState.value

                _uiState.value =
                    current.copy(
                        serverUrl =
                            if (
                                current.serverUrl.isBlank()
                            ) {
                                url
                            } else {
                                current.serverUrl
                            },

                        savedServerUrl = url,

                        hasUnsavedConnectionChanges =
                            current.serverUrl.trim() !=
                                url.trim()
                    )
            }
        }

        viewModelScope.launch {
            settings.authToken.collect { token ->

                _uiState.value =
                    _uiState.value.copy(
                        authToken =
                            token ?: ""
                    )
            }
        }

        viewModelScope.launch {
            settings.themeMode.collect { mode ->

                _uiState.value =
                    _uiState.value.copy(
                        themeMode = mode
                    )
            }
        }

        viewModelScope.launch {
            settings.isOnboarded.collect { onboarded ->

                _uiState.value =
                    _uiState.value.copy(
                        isOnboarded = onboarded
                    )
            }
        }
    }

    fun updateServerUrl(
        url: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                serverUrl = url,
                connectionVerified = false,
                testResult = null,
                hasUnsavedConnectionChanges =
                    url.trim() !=
                        _uiState.value.savedServerUrl.trim()
            )
    }

    fun updateAuthToken(
        token: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                authToken = token,
                connectionVerified = false,
                testResult = null
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
     * Test the currently entered configuration.
     *
     * Nothing is persisted by this method.
     */
    fun testConnection() {

        viewModelScope.launch {

            val current =
                _uiState.value

            val validation =
                ConnectionValidator.validate(
                    current.serverUrl
                )

            if (
                validation !is
                    com.lias.remote.core.network.ConnectionValidationResult.Valid
            ) {

                _uiState.value =
                    current.copy(
                        isTesting = false,
                        connectionVerified = false,
                        testResult =
                            validationMessage(
                                validation.reason
                            )
                    )

                return@launch
            }

            val normalizedUrl =
                validation.normalizedUrl

            val previousBaseUrl =
                api.baseUrl

            val previousAuthToken =
                api.authToken

            _uiState.value =
                current.copy(
                    isTesting = true,
                    connectionVerified = false,
                    testResult = null
                )

            try {

                api.baseUrl =
                    normalizedUrl

                api.authToken =
                    current.authToken
                        .trim()
                        .ifBlank {
                            null
                        }

                val result =
                    api.get<HealthResponse>(
                        Endpoints.HEALTH
                    )

                when (result) {

                    is ApiResult.Success -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting = false,
                                connectionVerified = true,
                                testResult =
                                    "Connection verified · Server ${result.data.version}"
                            )
                    }

                    is ApiResult.HttpError -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting = false,
                                connectionVerified = false,
                                testResult =
                                    httpErrorMessage(
                                        result.code
                                    )
                            )
                    }

                    is ApiResult.NetworkError -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting = false,
                                connectionVerified = false,
                                testResult =
                                    networkErrorMessage(
                                        result.cause.message
                                    )
                            )
                    }

                    is ApiResult.ConflictError -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting = false,
                                connectionVerified = false,
                                testResult =
                                    "The server rejected the connection request."
                            )
                    }
                }

            } finally {

                /*
                 * A test must not permanently redirect the shared API
                 * client unless the configuration is subsequently applied.
                 */
                if (
                    _uiState.value.savedServerUrl.trim() !=
                        normalizedUrl.trim()
                ) {

                    api.baseUrl =
                        previousBaseUrl

                    api.authToken =
                        previousAuthToken
                }
            }
        }
    }

    /**
     * Verify and persist a new connection.
     *
     * The callback is invoked only after persistence succeeds.
     */
    fun connect(
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            val current =
                _uiState.value

            val validation =
                ConnectionValidator.validate(
                    current.serverUrl
                )

            if (
                validation !is
                    com.lias.remote.core.network.ConnectionValidationResult.Valid
            ) {

                _uiState.value =
                    current.copy(
                        testResult =
                            validationMessage(
                                validation.reason
                            ),
                        connectionVerified = false
                    )

                return@launch
            }

            val normalizedUrl =
                validation.normalizedUrl

            val token =
                current.authToken
                    .trim()
                    .ifBlank {
                        null
                    }

            _uiState.value =
                current.copy(
                    isTesting = true,
                    isApplyingConnection = false,
                    connectionVerified = false,
                    testResult = null
                )

            val previousBaseUrl =
                api.baseUrl

            val previousAuthToken =
                api.authToken

            try {

                api.baseUrl =
                    normalizedUrl

                api.authToken =
                    token

                when (
                    val result =
                        api.get<HealthResponse>(
                            Endpoints.HEALTH
                        )
                ) {

                    is ApiResult.Success -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting = false,
                                isApplyingConnection = true,
                                connectionVerified = true,
                                testResult =
                                    "Connection verified"
                            )

                        settings.saveServerUrl(
                            normalizedUrl
                        )

                        settings.saveAuthToken(
                            token
                        )

                        _uiState.value =
                            _uiState.value.copy(
                                isApplyingConnection = false,
                                savedServerUrl =
                                    normalizedUrl,
                                serverUrl =
                                    normalizedUrl,
                                testResult =
                                    "Connected to LIAS · Server ${result.data.version}",
                                hasUnsavedConnectionChanges =
                                    false
                            )

                        onSuccess()
                    }

                    is ApiResult.HttpError -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting = false,
                                connectionVerified = false,
                                testResult =
                                    httpErrorMessage(
                                        result.code
                                    )
                            )

                        api.baseUrl =
                            previousBaseUrl

                        api.authToken =
                            previousAuthToken
                    }

                    is ApiResult.NetworkError -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting = false,
                                connectionVerified = false,
                                testResult =
                                    networkErrorMessage(
                                        result.cause.message
                                    )
                            )

                        api.baseUrl =
                            previousBaseUrl

                        api.authToken =
                            previousAuthToken
                    }

                    is ApiResult.ConflictError -> {

                        _uiState.value =
                            _uiState.value.copy(
                                isTesting = false,
                                connectionVerified = false,
                                testResult =
                                    "The server rejected the connection request."
                            )

                        api.baseUrl =
                            previousBaseUrl

                        api.authToken =
                            previousAuthToken
                    }
                }

            } catch (error: Exception) {

                api.baseUrl =
                    previousBaseUrl

                api.authToken =
                    previousAuthToken

                _uiState.value =
                    _uiState.value.copy(
                        isTesting = false,
                        isApplyingConnection = false,
                        connectionVerified = false,
                        testResult =
                            networkErrorMessage(
                                error.message
                            )
                    )
            }
        }
    }

    /**
     * Backward-compatible explicit save operation.
     *
     * Unlike the previous implementation, this never silently saves
     * an unverified connection.
     */
    fun saveSettings() {

        connect(
            onSuccess = {}
        )
    }

    fun toggleVacationMode(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            val result =
                eventRepository.toggleVacationMode(
                    enabled
                )

            if (
                result is ApiResult.Success
            ) {

                _uiState.value =
                    _uiState.value.copy(
                        vacationMode =
                            result.data.vacationMode
                    )
            }
        }
    }

    fun flushNftables() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isFlushing = true
                )

            eventRepository.flushNftables()

            _uiState.value =
                _uiState.value.copy(
                    isFlushing = false
                )
        }
    }

    private fun validationMessage(
        error: ConnectionValidationError
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

    private fun httpErrorMessage(
        code: Int
    ): String =
        when (code) {

            401 ->
                "The server requires a valid authentication token."

            403 ->
                "The server denied access."

            404 ->
                "This does not appear to be a LIAS server."

            in 500..599 ->
                "The LIAS server is temporarily unavailable."

            else ->
                "Server returned HTTP $code."
        }

    private fun networkErrorMessage(
        message: String?
    ): String {

        val detail =
            message
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        return if (
            detail == null
        ) {
            "Unable to reach the LIAS server."
        } else {
            "Unable to reach the LIAS server."
        }
    }
}
