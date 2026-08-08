// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/SettingsViewModel.kt
// Version: 4.0.0
//
// Purpose:
//   Connection, appearance, onboarding, and maintenance settings.
//
// Stability changes:
//   1. Exhaustive handling of ApiResult variants.
//   2. Test connection does not persist settings.
//   3. Authentication errors are clearly distinguished.
//   4. Serialization failures are not reported as network failures.
//   5. URL/token values are normalized before persistence.
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
    val testResult: String? = null,
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

    val uiState:
        StateFlow<SettingsUiState> =
        _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.serverUrl.collect { url ->
                _uiState.value =
                    _uiState.value.copy(
                        serverUrl =
                            if (
                                _uiState.value.serverUrl
                                    .isBlank()
                            ) {
                                url
                            } else {
                                _uiState.value.serverUrl
                            },
                        savedServerUrl = url
                    )
            }
        }

        viewModelScope.launch {
            settings.authToken.collect { token ->
                _uiState.value =
                    _uiState.value.copy(
                        authToken =
                            token.orEmpty()
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
                serverUrl = url
            )
    }

    fun updateAuthToken(
        token: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                authToken = token
            )
    }

    fun updateThemeMode(
        mode: String
    ) {
        val normalized =
            when (mode.lowercase()) {
                "light" -> "light"
                "dark" -> "dark"
                else -> "system"
            }

        viewModelScope.launch {
            settings.saveThemeMode(
                normalized
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

    fun testConnection() {
        viewModelScope.launch {

            val serverUrl =
                _uiState.value.serverUrl
                    .trim()

            if (serverUrl.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(
                        isTesting = false,
                        testResult =
                            "Enter the LIAS server address first."
                    )
                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    isTesting = true,
                    testResult = null
                )

            api.baseUrl =
                serverUrl

            api.authToken =
                _uiState.value.authToken
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    }

            val result =
                api.get<HealthResponse>(
                    Endpoints.HEALTH
                )

            val message =
                when (result) {
                    is ApiResult.Success ->
                        "Connection successful · Server ${result.data.version}"

                    is ApiResult.AuthenticationError ->
                        "Authentication required (${result.code})."

                    is ApiResult.HttpError ->
                        "Server returned HTTP ${result.code}: ${result.message}"

                    is ApiResult.ConflictError ->
                        result.message

                    is ApiResult.NetworkError ->
                        result.cause.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Unable to reach the server."

                    is ApiResult.SerializationError ->
                        "The server response could not be understood."
                }

            _uiState.value =
                _uiState.value.copy(
                    isTesting = false,
                    testResult = message
                )
        }
    }

    fun saveSettings() {
        viewModelScope.launch {

            val normalizedUrl =
                _uiState.value.serverUrl
                    .trim()

            val normalizedToken =
                _uiState.value.authToken
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    }

            settings.saveServerUrl(
                normalizedUrl
            )

            settings.saveAuthToken(
                normalizedToken
            )

            _uiState.value =
                _uiState.value.copy(
                    serverUrl = normalizedUrl,
                    authToken =
                        normalizedToken.orEmpty(),
                    savedServerUrl =
                        normalizedUrl,
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
                eventRepository.toggleVacationMode(
                    enabled
                )

            if (result is ApiResult.Success) {
                _uiState.value =
                    _uiState.value.copy(
                        vacationMode =
                            result.data.vacationMode
                    )
            } else {
                val message =
                    when (result) {
                        is ApiResult.AuthenticationError ->
                            result.message

                        is ApiResult.HttpError ->
                            result.message

                        is ApiResult.ConflictError ->
                            result.message

                        is ApiResult.NetworkError ->
                            result.cause.message
                                ?: "Unable to change Vacation Mode."

                        is ApiResult.SerializationError ->
                            "The server returned an invalid response."

                        is ApiResult.Success ->
                            null
                    }

                eventRepository.emitUiEvent(
                    com.lias.remote.repositories.UiEvent.ShowSnackbarError(
                        message
                            ?: "Unable to change Vacation Mode."
                    )
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

            val result =
                eventRepository.flushNftables()

            _uiState.value =
                _uiState.value.copy(
                    isFlushing = false
                )

            if (result !is ApiResult.Success) {
                val message =
                    when (result) {
                        is ApiResult.AuthenticationError ->
                            result.message

                        is ApiResult.HttpError ->
                            result.message

                        is ApiResult.ConflictError ->
                            result.message

                        is ApiResult.NetworkError ->
                            result.cause.message
                                ?: "Unable to flush firewall state."

                        is ApiResult.SerializationError ->
                            "The server returned an invalid response."

                        is ApiResult.Success ->
                            null
                    }

                eventRepository.emitUiEvent(
                    com.lias.remote.repositories.UiEvent.ShowSnackbarError(
                        message
                            ?: "Unable to flush firewall state."
                    )
                )
            }
        }
    }
}
