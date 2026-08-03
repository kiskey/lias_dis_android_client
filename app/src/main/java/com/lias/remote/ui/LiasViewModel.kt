// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 1.1.1
// Audit Fixes: 
//   1. Added deletePolicy() and deleteSchedule() functions to match UI wiring.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.network.ApiResult
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.UiState
import kotlinx.coroutines.flow.StateFlow
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

    // FIX 3.2: Added deletePolicy
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

    // FIX 3.2: Added deleteSchedule
    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            val result = eventRepository.deleteSchedule(scheduleId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                // Emit Snackbar error
            }
        }
    }
}
