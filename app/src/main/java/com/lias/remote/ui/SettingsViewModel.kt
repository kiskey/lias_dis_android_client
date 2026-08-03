// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/SettingsViewModel.kt
// Version: 1.2.0
// Audit Fixes: 
//   1. Added `flushNftables()` function to support Danger Zone UI (Gap 3.2).
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.HealthResponse
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
    val testResult: String? = null,
    val isFlushing: Boolean = false 
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
            
            val tempUrl = _uiState.value.serverUrl
            val tempToken = _uiState.value.authToken.ifBlank { null }
            
            api.baseUrl = tempUrl
            api.authToken = tempToken

            val result = api.get<HealthResponse>(Endpoints.HEALTH)
            
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = when (result) {
                    is ApiResult.Success -> "Connection successful! Server Version: ${result.data.version}"
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

    fun flushNftables() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFlushing = true)
            val result = api.post<Unit, Unit>(Endpoints.NFTABLES_FLUSH, Unit)
            _uiState.value = _uiState.value.copy(
                isFlushing = false,
                testResult = when (result) {
                    is ApiResult.Success -> "Nftables table flushed successfully."
                    is ApiResult.HttpError -> "Flush failed: HTTP ${result.code}"
                    is ApiResult.NetworkError -> "Flush failed: ${result.cause.message}"
                    is ApiResult.Conflict -> "Conflict"
                }
            )
        }
    }
}
