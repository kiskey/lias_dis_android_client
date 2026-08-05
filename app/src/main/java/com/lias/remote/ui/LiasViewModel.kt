// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 1.8.0
// Audit Fixes: 
//   1. Added ViewModel wrapper methods for all extended repository actions:
//      pauseDeviceInternet, unpauseDeviceInternet, renameDevice, getDeviceLogs,
//      assignDeviceTags, exportPolicies, importPolicies, createUser, and assignDeviceUser.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.UiEvent
import com.lias.remote.repositories.UiState
import com.lias.remote.repositories.assignDeviceTag
import com.lias.remote.repositories.assignDeviceTags
import com.lias.remote.repositories.assignDeviceUser
import com.lias.remote.repositories.createTag
import com.lias.remote.repositories.createUser
import com.lias.remote.repositories.deletePolicy
import com.lias.remote.repositories.deleteSchedule
import com.lias.remote.repositories.deleteTag
import com.lias.remote.repositories.exportPolicies
import com.lias.remote.repositories.getDeviceLogs
import com.lias.remote.repositories.importPolicies
import com.lias.remote.repositories.pauseDeviceInternet
import com.lias.remote.repositories.renameDevice
import com.lias.remote.repositories.savePolicy
import com.lias.remote.repositories.saveSchedule
import com.lias.remote.repositories.unpauseDeviceInternet
import com.lias.remote.repositories.updateTag
import com.lias.remote.repositories.validatePolicy
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

    suspend fun validatePolicy(scheduleIds: List<String>): ApiResult<List<Conflict>> {
        return eventRepository.validatePolicy(scheduleIds)
    }

    fun assignTags(pdid: String, tagIds: List<String>) {
        viewModelScope.launch {
            val result = eventRepository.assignDeviceTags(pdid, tagIds)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to assign tags: $msg"))
            }
        }
    }

    fun assignTag(pdid: String, tagId: String) {
        viewModelScope.launch {
            val result = eventRepository.assignDeviceTag(pdid, tagId)
            if (result is ApiResult.HttpError || result is ApiResult.NetworkError) {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to assign tag: $msg"))
            }
        }
    }

    fun pauseInternet(pdid: String) {
        viewModelScope.launch {
            val result = eventRepository.pauseDeviceInternet(pdid)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("⏸ Internet paused for 1 hour"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to pause internet: $msg"))
            }
        }
    }

    fun unpauseInternet(pdid: String) {
        viewModelScope.launch {
            val result = eventRepository.unpauseDeviceInternet(pdid)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("▶ Internet resumed"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to unpause internet: $msg"))
            }
        }
    }

    fun renameDevice(pdid: String, newName: String) {
        viewModelScope.launch {
            val result = eventRepository.renameDevice(pdid, newName)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("✏️ Device renamed to $newName"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to rename device: $msg"))
            }
        }
    }

    suspend fun getDeviceLogs(pdid: String): ApiResult<List<FlowLog>> {
        return eventRepository.getDeviceLogs(pdid)
    }

    fun exportPolicies(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = eventRepository.exportPolicies()
            if (result is ApiResult.Success) {
                onSuccess(result.data)
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Policies exported successfully"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to export policies: $msg"))
            }
        }
    }

    fun importPolicies(jsonPayload: String) {
        viewModelScope.launch {
            val result = eventRepository.importPolicies(jsonPayload)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Policies imported successfully"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to import policies: $msg"))
            }
        }
    }

    fun createUser(name: String) {
        viewModelScope.launch {
            val user = User(id = "user_${System.currentTimeMillis()}", name = name)
            val result = eventRepository.createUser(user)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("User profile created"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to create user: $msg"))
            }
        }
    }

    fun assignUser(pdid: String, userId: String) {
        viewModelScope.launch {
            val result = eventRepository.assignDeviceUser(pdid, userId)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("User profile assigned"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to assign user: $msg"))
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
