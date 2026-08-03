// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/SettingsViewModel.kt
// Version: 1.0.0
// Purpose: ViewModel for managing server connection settings and
//          validating them before starting the main EventRepository.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.LiasApiClient
import com.lias.remote.core.store.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val authToken: String = "",
    val isTesting: Boolean = false,
    val testResult: String? = null
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val api: LiasApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.serverUrl.collect { url ->
                _uiState.value = _uiState.value.copy(serverUrl = url)
            }
        }
        viewModelScope.launch {
            settings.authToken.collect { token ->
                _uiState.value = _uiState.value.copy(authToken = token ?: "")
            }
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
    }

    fun updateAuthToken(token: String) {
        _uiState.value = _uiState.value.copy(authToken = token)
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            
            // Temporarily apply settings to API client
            val tempUrl = _uiState.value.serverUrl
            val tempToken = _uiState.value.authToken.ifBlank { null }
            
            api.baseUrl = tempUrl
            api.authToken = tempToken

            val result = api.get<com.lias.remote.core.network.DeviceListResponse>(Endpoints.HEALTH)
            
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = when (result) {
                    is ApiResult.Success -> "Connection successful!"
                    is ApiResult.HttpError -> "HTTP Error: ${result.code}"
                    is ApiResult.NetworkError -> "Failed: ${result.cause.message}"
                    is ApiResult.Conflict -> "Conflict"
                }
            )
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            settings.saveServerUrl(_uiState.value.serverUrl)
            settings.saveAuthToken(_uiState.value.authToken.ifBlank { null })
        }
    }
}
