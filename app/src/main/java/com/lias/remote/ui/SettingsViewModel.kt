// ====================================================================
// File: SettingsViewModel.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Exposed Onboarding state to trigger OnboardingSheet.
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

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.serverUrl.collect { url ->
                _uiState.value = _uiState.value.copy(
                    serverUrl = if (_uiState.value.serverUrl.isBlank()) url else _uiState.value.serverUrl,
                    savedServerUrl = url
                )
            }
        }
        viewModelScope.launch {
            settings.authToken.collect { token ->
                _uiState.value = _uiState.value.copy(authToken = token ?: "")
            }
        }
        viewModelScope.launch {
            settings.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            settings.isOnboarded.collect { onboarded ->
                _uiState.value = _uiState.value.copy(isOnboarded = onboarded)
            }
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
    }

    fun updateAuthToken(token: String) {
        _uiState.value = _uiState.value.copy(authToken = token)
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { settings.saveThemeMode(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboarded(true) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            api.baseUrl = _uiState.value.serverUrl
            api.authToken = _uiState.value.authToken.ifBlank { null }
            val result = api.get<HealthResponse>(Endpoints.HEALTH)
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = when (result) {
                    is ApiResult.Success -> "Connection successful! Server Version: ${result.data.version}"
                    is ApiResult.HttpError -> "HTTP Error: ${result.code}"
                    is ApiResult.NetworkError -> "Failed: ${result.cause.message}"
                    is ApiResult.ConflictError -> "Conflict"
                }
            )
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val urlToSave = _uiState.value.serverUrl.trim()
            settings.saveServerUrl(urlToSave)
            settings.saveAuthToken(_uiState.value.authToken.ifBlank { null })
            _uiState.value = _uiState.value.copy(
                savedServerUrl = urlToSave,
                testResult = "Settings saved successfully."
            )
        }
    }

    fun toggleVacationMode(enabled: Boolean) {
        viewModelScope.launch {
            val result = eventRepository.toggleVacationMode(enabled)
            if (result is ApiResult.Success) {
                _uiState.value = _uiState.value.copy(vacationMode = result.data.vacationMode)
            }
        }
    }

    fun flushNftables() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFlushing = true)
            val result = eventRepository.flushNftables()
            _uiState.value = _uiState.value.copy(isFlushing = false)
        }
    }
}
