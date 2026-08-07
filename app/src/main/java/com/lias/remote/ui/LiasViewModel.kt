// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 2.5.0
// Audit Fixes:
//   1. Explicitly imported UiState and UiEvent from repositories package.
//   2. Typed inFlightPauseRequests as MutableSet<String> for clean Kotlin interop.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.util.ExtendHelper
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.UiEvent
import com.lias.remote.repositories.UiState
import com.lias.remote.repositories.assignDeviceTag
import com.lias.remote.repositories.assignDeviceTags
import com.lias.remote.repositories.assignDeviceUser
import com.lias.remote.repositories.cancelDeviceExtension
import com.lias.remote.repositories.cancelTagExtension
import com.lias.remote.repositories.createTag
import com.lias.remote.repositories.createUser
import com.lias.remote.repositories.deletePolicy
import com.lias.remote.repositories.deleteSchedule
import com.lias.remote.repositories.deleteTag
import com.lias.remote.repositories.exportPolicies
import com.lias.remote.repositories.extendDeviceAccess
import com.lias.remote.repositories.extendTagAccess
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
import java.util.concurrent.ConcurrentHashMap

class LiasViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    val state: StateFlow<UiState> = eventRepository.state
    val uiEvents = eventRepository.uiEvents

    private val inFlightPauseRequests: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        eventRepository.start()
    }

    fun extendDeviceAccess(pdid: String, minutes: Int) {
        viewModelScope.launch {
            val result = eventRepository.extendDeviceAccess(pdid, minutes)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("⏳ Access extended by $minutes minutes"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to extend access: $msg"))
            }
        }
    }

    fun cancelDeviceExtension(pdid: String) {
        viewModelScope.launch {
            val result = eventRepository.cancelDeviceExtension(pdid)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Extension cancelled"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to cancel extension: $msg"))
            }
        }
    }

    fun extendTagAccess(tagId: String, minutes: Int) {
        viewModelScope.launch {
            val result = eventRepository.extendTagAccess(tagId, minutes)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("⏳ Tag access extended by $minutes minutes"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to extend tag access: $msg"))
            }
        }
    }

    fun cancelTagExtension(tagId: String) {
        viewModelScope.launch {
            val result = eventRepository.cancelTagExtension(tagId)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Tag extension cancelled"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to cancel tag extension: $msg"))
            }
        }
    }

    fun effectiveStatusFor(pdid: String): EffectiveStatus {
        state.value.deviceEffectiveStatuses[pdid]?.let { return it }

        val device = state.value.devices.find { it.pdid == pdid }
        if (device != null && device.safeTags.contains("infrastructure")) {
            return EffectiveStatus(action = "allow", source = "infrastructure", extendAvailable = false, pauseAvailable = false)
        }
        val extensionPol = state.value.policies.find { it.id == "pol_extend_device_$pdid" }
        if (extensionPol != null && extensionPol.enabled) {
            val minsLeft = ExtendHelper.minutesUntil(extensionPol.expiresAt)
            if (minsLeft > 0) {
                return EffectiveStatus(
                    action = "allow",
                    source = "device_policy",
                    extendAvailable = false,
                    pauseAvailable = true,
                    activeExtension = ExtensionInfo(
                        expiresAt = extensionPol.expiresAt ?: "",
                        minutesLeft = minsLeft,
                        reasonTag = extensionPol.reasonTag ?: "extend_access"
                    )
                )
            }
        }
        val pausePol = state.value.policies.find { it.id == "pol_pause_$pdid" }
        if (pausePol != null && pausePol.enabled) {
            val minsLeft = ExtendHelper.minutesUntil(pausePol.expiresAt)
            if (minsLeft > 0) {
                return EffectiveStatus(
                    action = "block",
                    source = "device_policy",
                    extendAvailable = true,
                    pauseAvailable = false,
                    activeExtension = ExtensionInfo(
                        expiresAt = pausePol.expiresAt ?: "",
                        minutesLeft = minsLeft,
                        reasonTag = pausePol.reasonTag ?: "pause"
                    )
                )
            }
        }
        return EffectiveStatus(action = "block", source = "schedule", extendAvailable = true, pauseAvailable = true)
    }

    fun effectiveStatusForTag(tagId: String): EffectiveStatus {
        state.value.tagEffectiveStatuses[tagId]?.let { return it }

        if (tagId == "infrastructure") {
            return EffectiveStatus(action = "allow", source = "infrastructure", extendAvailable = false, pauseAvailable = false)
        }
        val extensionPol = state.value.policies.find { it.id == "pol_extend_tag_$tagId" }
        if (extensionPol != null && extensionPol.enabled) {
            val minsLeft = ExtendHelper.minutesUntil(extensionPol.expiresAt)
            if (minsLeft > 0) {
                return EffectiveStatus(
                    action = "allow",
                    source = "tag_policy",
                    extendAvailable = false,
                    pauseAvailable = true,
                    activeExtension = ExtensionInfo(
                        expiresAt = extensionPol.expiresAt ?: "",
                        minutesLeft = minsLeft,
                        reasonTag = extensionPol.reasonTag ?: "extend_access"
                    )
                )
            }
        }
        return EffectiveStatus(action = "block", source = "schedule", extendAvailable = true, pauseAvailable = true)
    }

    suspend fun validatePolicy(scheduleIds: List<String>): ApiResult<List<Conflict>> {
        return eventRepository.validatePolicy(scheduleIds)
    }

    fun assignTags(pdid: String, tagIds: List<String>) {
        viewModelScope.launch {
            val result = eventRepository.assignDeviceTags(pdid, tagIds)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Tags updated successfully"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to assign tags: $msg"))
            }
        }
    }

    fun assignTag(pdid: String, tagId: String) {
        assignTags(pdid, listOf(tagId))
    }

    fun pauseInternet(pdid: String) {
        viewModelScope.launch {
            val isPaused = state.value.policies.any { it.id == "pol_pause_$pdid" }
            if (isPaused) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("⏸ Internet is already paused for this device"))
                return@launch
            }
            if (inFlightPauseRequests.contains(pdid)) {
                return@launch
            }
            inFlightPauseRequests.add(pdid)
            try {
                val result = eventRepository.pauseDeviceInternet(pdid)
                if (result is ApiResult.Success) {
                    eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("⏸ Internet paused for 1 hour"))
                } else {
                    val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                    eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to pause internet: $msg"))
                }
            } finally {
                inFlightPauseRequests.remove(pdid)
            }
        }
    }

    fun unpauseInternet(pdid: String) {
        viewModelScope.launch {
            val isPaused = state.value.policies.any { it.id == "pol_pause_$pdid" }
            if (!isPaused) {
                return@launch
            }
            if (inFlightPauseRequests.contains(pdid)) {
                return@launch
            }
            inFlightPauseRequests.add(pdid)
            try {
                val result = eventRepository.unpauseDeviceInternet(pdid)
                if (result is ApiResult.Success) {
                    eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("▶ Internet resumed"))
                } else {
                    val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                    eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to unpause internet: $msg"))
                }
            } finally {
                inFlightPauseRequests.remove(pdid)
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
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Tag created successfully"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to create tag: $msg"))
            }
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            val result = eventRepository.updateTag(tag)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Tag updated successfully"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to update tag: $msg"))
            }
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            val result = eventRepository.deleteTag(tagId)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Tag deleted"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to delete tag: $msg"))
            }
        }
    }

    fun savePolicy(policy: Policy) {
        viewModelScope.launch {
            val result = eventRepository.savePolicy(policy)
            if (result is ApiResult.Success) {
                val toastMsg = if (policy.id == "global_default") {
                    "Global Access Switch set to: ${policy.action.uppercase()}"
                } else {
                    "Policy saved successfully"
                }
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar(toastMsg))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to save policy: $msg"))
            }
        }
    }

    fun deletePolicy(policyId: String) {
        viewModelScope.launch {
            val result = eventRepository.deletePolicy(policyId)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Policy deleted"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to delete policy: $msg"))
            }
        }
    }

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val result = eventRepository.saveSchedule(schedule)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Schedule saved successfully"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to save schedule: $msg"))
            }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            val result = eventRepository.deleteSchedule(scheduleId)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Schedule deleted"))
            } else {
                val msg = (result as? ApiResult.HttpError)?.message ?: (result as? ApiResult.NetworkError)?.cause?.message ?: "Network Error"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbarError("Failed to delete schedule: $msg"))
            }
        }
    }
}
