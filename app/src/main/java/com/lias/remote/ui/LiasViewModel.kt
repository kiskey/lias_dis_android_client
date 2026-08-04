// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 1.4.0
// Audit Fixes: 
//   1. Emit Error Snackbars on API failures (GAP-U43).
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
            // GAP-U43 Fix: Emit Error Snackbar
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to assign tag: $msg"))
            }
        }
    }

    fun createTag(tag: Tag) {
        viewModelScope.launch {
            val result = eventRepository.createTag(tag)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to create tag: $msg"))
            }
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            val result = eventRepository.updateTag(tag)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to update tag: $msg"))
            }
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            val result = eventRepository.deleteTag(tagId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to delete tag: $msg"))
            }
        }
    }

    fun savePolicy(policy: Policy) {
        viewModelScope.launch {
            val result = eventRepository.savePolicy(policy)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to save policy: $msg"))
            }
        }
    }

    fun deletePolicy(policyId: String) {
        viewModelScope.launch {
            val result = eventRepository.deletePolicy(policyId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to delete policy: $msg"))
            }
        }
    }

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val result = eventRepository.saveSchedule(schedule)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to save schedule: $msg"))
            }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            val result = eventRepository.deleteSchedule(scheduleId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to delete schedule: $msg"))
            }
        }
    }
}
