// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 1.2.0
// Audit Fixes: 
//   1. Exposed `uiEvents` flow to allow UI layer to collect Snackbar events.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.network.ApiResult
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.UiEvent
import com.lias.remote.repositories.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LiasViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    val state: StateFlow<UiState> = eventRepository.state

    // FIX 3.1: Expose transient UI events
    val uiEvents = eventRepository.uiEvents

    init {
        eventRepository.start()
    }

    fun assignTag(pdid: String, tagId: String) {
        viewModelScope.launch {
            val result = eventRepository.assignDeviceTag(pdid, tagId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                // Could emit a specific error snackbar here
            }
        }
    }

    fun savePolicy(policy: com.lias.remote.core.models.Policy) {
        viewModelScope.launch {
            eventRepository.savePolicy(policy)
        }
    }

    fun deletePolicy(policyId: String) {
        viewModelScope.launch {
            val result = eventRepository.deletePolicy(policyId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                // Emit Snackbar error
            }
        }
    }

    fun saveSchedule(schedule: com.lias.remote.core.models.Schedule) {
        viewModelScope.launch {
            eventRepository.saveSchedule(schedule)
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            val result = eventRepository.deleteSchedule(scheduleId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                // Emit Snackbar error
            }
        }
    }
}
