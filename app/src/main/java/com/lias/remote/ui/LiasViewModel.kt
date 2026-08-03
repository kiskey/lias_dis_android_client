// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 1.0.0
// Purpose: Primary ViewModel for screens requiring global app state.
//          Bridges the EventRepository to Jetpack Compose lifecycle.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.network.ApiResult
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.UiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LiasViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    val state: StateFlow<UiState> = eventRepository.state

    init {
        eventRepository.start()
    }

    fun assignTag(pdid: String, tagId: String) {
        viewModelScope.launch {
            val result = eventRepository.assignDeviceTag(pdid, tagId)
            // UI can observe state.errorMessage if needed
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                // In a full app, emit a transient error event to show a Snackbar
            }
        }
    }

    fun savePolicy(policy: com.lias.remote.core.models.Policy) {
        viewModelScope.launch {
            eventRepository.savePolicy(policy)
        }
    }

    fun saveSchedule(schedule: com.lias.remote.core.models.Schedule) {
        viewModelScope.launch {
            eventRepository.saveSchedule(schedule)
        }
    }
}
