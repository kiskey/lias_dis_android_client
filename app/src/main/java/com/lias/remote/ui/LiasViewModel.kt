// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 1.3.0
// Audit Fixes: 
//   1. Exposed Tag CRUD actions to UI layer (GAP-C03).
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.UiEvent
import com.lias.remote.repositories.UiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LiasViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    val state: StateFlow<UiState> = eventRepository.state
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

    // GAP-C03 Fix: Tag CRUD Methods
    fun createTag(tag: Tag) {
        viewModelScope.launch {
            val result = eventRepository.createTag(tag)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                // Emit Snackbar error
            }
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            val result = eventRepository.updateTag(tag)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                // Emit Snackbar error
            }
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            val result = eventRepository.deleteTag(tagId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                // Emit Snackbar error
            }
        }
    }

    fun savePolicy(policy: Policy) {
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

    fun saveSchedule(schedule: Schedule) {
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
