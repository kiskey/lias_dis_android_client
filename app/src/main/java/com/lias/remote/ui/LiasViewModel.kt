package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.SecurityAlertPayload
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.UiEvent
import com.lias.remote.repositories.UiState
import com.lias.remote.repositories.assignDeviceTags
import com.lias.remote.repositories.assignDeviceUser
import com.lias.remote.repositories.createUser
import com.lias.remote.repositories.deletePolicy
import com.lias.remote.repositories.deleteSchedule
import com.lias.remote.repositories.deleteTag
import com.lias.remote.repositories.exportPolicies
import com.lias.remote.repositories.extendDeviceAccess
import com.lias.remote.repositories.getDeviceLogs
import com.lias.remote.repositories.renameDevice
import com.lias.remote.repositories.savePolicy
import com.lias.remote.repositories.saveSchedule
import com.lias.remote.repositories.unpauseDeviceInternet
import com.lias.remote.repositories.updateTag
import com.lias.remote.ui.components.UndoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class LiasViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    val state: StateFlow<UiState> = eventRepository.state
    val uiEvents = eventRepository.uiEvents

    private val _pendingSecurityAlert = MutableStateFlow<SecurityAlertPayload?>(null)
    val pendingSecurityAlert: StateFlow<SecurityAlertPayload?> = _pendingSecurityAlert.asStateFlow()

    private val _undoState = MutableStateFlow<UndoState?>(null)
    val undoState: StateFlow<UndoState?> = _undoState.asStateFlow()

    init {
        eventRepository.start()
        viewModelScope.launch {
            eventRepository.uiEvents.collect { event ->
                if (event is UiEvent.ShowSecurityAlert) {
                    _pendingSecurityAlert.value = SecurityAlertPayload(
                        alertType = "Anomaly Detected",
                        details = event.details,
                        pdid = "",
                        timestamp = Instant.now().toString()
                    )
                }
            }
        }
    }

    fun dismissSecurityAlert() { _pendingSecurityAlert.value = null }
    fun clearUndo() { _undoState.value = null }

    fun extendDeviceAccess(pdid: String, minutes: Int) {
        viewModelScope.launch {
            val result = eventRepository.extendDeviceAccess(pdid, minutes)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("⏳ Access extended by $minutes minutes"))
            }
        }
    }

    fun cancelDeviceExtension(pdid: String) {
        viewModelScope.launch { eventRepository.cancelDeviceExtension(pdid) }
    }

    fun effectiveStatusFor(pdid: String): EffectiveStatus {
        return state.value.deviceEffectiveStatuses[pdid] ?: EffectiveStatus(action = "block", extendAvailable = true)
    }

    fun assignTags(pdid: String, tagIds: List<String>) {
        val prevTags = state.value.devices.find { it.pdid == pdid }?.tags ?: listOf("generic")
        viewModelScope.launch {
            val result = eventRepository.assignDeviceTags(pdid, tagIds)
            if (result is ApiResult.Success) {
                _undoState.value = UndoState("Tags updated") {
                    viewModelScope.launch { eventRepository.assignDeviceTags(pdid, prevTags) }
                }
            }
        }
    }

    fun pauseDeviceInternet(pdid: String, minutes: Int) {
        viewModelScope.launch {
            val expiresAt = Instant.now().plusSeconds(minutes * 60L).toString()
            val pol = Policy(
                id = "pol_pause_$pdid",
                name = "Paused Internet",
                type = "device",
                targetID = pdid,
                action = "block",
                priority = 2000,
                enabled = true,
                expiresAt = expiresAt,
                reasonTag = "pause",
                createdAt = Instant.now().toString(),
                updatedAt = Instant.now().toString()
            )
            val result = eventRepository.savePolicy(pol)
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("⏸ Internet paused for $minutes min"))
            }
        }
    }

    fun unpauseDeviceInternet(pdid: String) {
        viewModelScope.launch { eventRepository.unpauseDeviceInternet(pdid) }
    }

    fun renameDevice(pdid: String, newName: String) {
        val prevName = state.value.devices.find { it.pdid == pdid }?.friendlyName ?: ""
        viewModelScope.launch {
            val result = eventRepository.renameDevice(pdid, newName)
            if (result is ApiResult.Success) {
                _undoState.value = UndoState("Device renamed") {
                    viewModelScope.launch { eventRepository.renameDevice(pdid, prevName) }
                }
            }
        }
    }

    fun assignDeviceUser(pdid: String, userId: String) {
        viewModelScope.launch { eventRepository.assignDeviceUser(pdid, userId) }
    }

    fun createUser(user: User) {
        viewModelScope.launch { eventRepository.createUser(user) }
    }

    fun exportPolicies() {
        viewModelScope.launch {
            val result = eventRepository.exportPolicies()
            if (result is ApiResult.Success) {
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar("Policies exported successfully"))
            }
        }
    }

    fun savePolicy(policy: Policy) {
        viewModelScope.launch {
            val result = eventRepository.savePolicy(policy)
            if (result is ApiResult.Success) {
                val msg = if (policy.id == "global_default") "Global Switch updated" else "Rule saved"
                eventRepository._uiEvents.emit(UiEvent.ShowSnackbar(msg))
            }
        }
    }

    fun deletePolicy(policyId: String, policyName: String, policy: Policy) {
        viewModelScope.launch {
            val result = eventRepository.deletePolicy(policyId)
            if (result is ApiResult.Success) {
                _undoState.value = UndoState("Rule '$policyName' deleted") {
                    viewModelScope.launch { eventRepository.savePolicy(policy) }
                }
            }
        }
    }

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch { eventRepository.saveSchedule(schedule) }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch { eventRepository.deleteSchedule(scheduleId) }
    }

    fun createTag(tag: Tag) {
        viewModelScope.launch { eventRepository.createTag(tag) }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch { eventRepository.updateTag(tag) }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch { eventRepository.deleteTag(tagId) }
    }

    suspend fun getDeviceLogs(pdid: String): ApiResult<List<FlowLog>> {
        return eventRepository.getDeviceLogs(pdid)
    }
}
