// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 6.0.0
//
// Purpose:
//   Complete UI-facing façade for LIAS Remote.
//
// Guarantees:
//   - Repository remains source of truth.
//   - UI cannot mutate repository internals.
//   - Full existing action surface remains available.
//   - Effective status is nullable until actually known.
//   - Refresh is explicitly exposed to state/error screens.
// ====================================================================

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
import com.lias.remote.repositories.cancelDeviceExtension
import com.lias.remote.repositories.createTag
import com.lias.remote.repositories.createUser
import com.lias.remote.repositories.deletePolicy
import com.lias.remote.repositories.deleteSchedule
import com.lias.remote.repositories.deleteTag
import com.lias.remote.repositories.extendDeviceAccess
import com.lias.remote.repositories.exportPolicies
import com.lias.remote.repositories.getDeviceLogs
import com.lias.remote.repositories.importPolicies
import com.lias.remote.repositories.pauseDeviceInternet
import com.lias.remote.repositories.renameDevice
import com.lias.remote.repositories.savePolicy
import com.lias.remote.repositories.saveSchedule
import com.lias.remote.repositories.toggleVacationMode
import com.lias.remote.repositories.unpauseDeviceInternet
import com.lias.remote.repositories.updateTag
import com.lias.remote.ui.components.UndoState
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiasViewModel(
    private val eventRepository:
        EventRepository
) : ViewModel() {

    val state: StateFlow<UiState> =
        eventRepository.state

    val uiEvents =
        eventRepository.uiEvents

    private val _pendingSecurityAlert =
        MutableStateFlow<
            SecurityAlertPayload?
        >(null)

    val pendingSecurityAlert:
        StateFlow<SecurityAlertPayload?> =
        _pendingSecurityAlert
            .asStateFlow()

    private val _undoState =
        MutableStateFlow<
            UndoState?
        >(null)

    val undoState:
        StateFlow<UndoState?> =
        _undoState.asStateFlow()

    init {
        eventRepository.start()

        viewModelScope.launch {
            eventRepository.uiEvents
                .collect { event ->

                    if (
                        event is
                            UiEvent.ShowSecurityAlert
                    ) {
                        _pendingSecurityAlert.value =
                            SecurityAlertPayload(
                                alertType =
                                    "security_alert",
                                details =
                                    event.details,
                                pdid =
                                    event.pdid,
                                timestamp =
                                    Instant.now()
                                        .toString()
                            )
                    }
                }
        }
    }

    // ----------------------------------------------------------------
    // Synchronization
    // ----------------------------------------------------------------

    fun refresh() {
        viewModelScope.launch {
            eventRepository.refreshAll()
        }
    }

    fun clearError() {
        eventRepository.clearError()
    }

    // ----------------------------------------------------------------
    // Security
    // ----------------------------------------------------------------

    fun triggerSecurityAlert() {
        _pendingSecurityAlert.value =
            SecurityAlertPayload(
                alertType =
                    "mac_spoof_detected",
                details =
                    "Potential network identity anomaly detected.",
                pdid =
                    state.value.devices
                        .firstOrNull()
                        ?.pdid
                        .orEmpty(),
                timestamp =
                    Instant.now()
                        .toString()
            )
    }

    fun dismissSecurityAlert() {
        _pendingSecurityAlert.value =
            null
    }

    // ----------------------------------------------------------------
    // Undo
    // ----------------------------------------------------------------

    fun clearUndo() {
        _undoState.value =
            null
    }

    // ----------------------------------------------------------------
    // Effective state
    // ----------------------------------------------------------------

    fun effectiveStatusFor(
        pdid: String
    ): EffectiveStatus? =
        state.value
            .deviceEffectiveStatuses[
                pdid
            ]

    fun tagEffectiveStatusFor(
        tagId: String
    ): EffectiveStatus? =
        state.value
            .tagEffectiveStatuses[
                tagId
            ]

    // ----------------------------------------------------------------
    // Device actions
    // ----------------------------------------------------------------

    fun extendDeviceAccess(
        pdid: String,
        minutes: Int
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .extendDeviceAccess(
                        pdid,
                        minutes
                    )

            if (
                result is ApiResult.Success
            ) {
                eventRepository.emitUiEvent(
                    UiEvent.ShowSnackbar(
                        "Access extended by $minutes minutes"
                    )
                )
            } else {
                emitFailure(
                    result,
                    "Unable to extend access."
                )
            }
        }
    }

    fun cancelDeviceExtension(
        pdid: String
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .cancelDeviceExtension(
                        pdid
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to cancel extension."
                )
            }
        }
    }

    fun assignTags(
        pdid: String,
        tagIds: List<String>
    ) {
        val previous =
            state.value.devices
                .find {
                    it.pdid == pdid
                }
                ?.safeTags
                .orEmpty()

        viewModelScope.launch {
            val result =
                eventRepository.assignDeviceTags(
                    pdid,
                    tagIds
                )

            if (
                result is ApiResult.Success
            ) {
                _undoState.value =
                    UndoState(
                        "Tags updated"
                    ) {
                        viewModelScope.launch {
                            eventRepository
                                .assignDeviceTags(
                                    pdid,
                                    previous
                                )
                        }
                    }
            } else {
                emitFailure(
                    result,
                    "Unable to update tags."
                )
            }
        }
    }

    fun pauseDeviceInternet(
        pdid: String,
        minutes: Int
    ) {
        if (minutes <= 0) {
            viewModelScope.launch {
                eventRepository.emitUiEvent(
                    UiEvent.ShowSnackbarError(
                        "Choose a valid pause duration."
                    )
                )
            }
            return
        }

        /*
         * Backend pause remains authoritative. The supplied API exposes
         * pause as the mutation while the UI duration is retained for
         * the existing PauseSheet contract.
         */
        viewModelScope.launch {
            val result =
                eventRepository
                    .pauseDeviceInternet(
                        pdid
                    )

            if (
                result is ApiResult.Success
            ) {
                eventRepository.emitUiEvent(
                    UiEvent.ShowSnackbar(
                        "Internet paused"
                    )
                )
            } else {
                emitFailure(
                    result,
                    "Unable to pause internet."
                )
            }
        }
    }

    fun unpauseDeviceInternet(
        pdid: String
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .unpauseDeviceInternet(
                        pdid
                    )

            if (
                result is ApiResult.Success
            ) {
                eventRepository.emitUiEvent(
                    UiEvent.ShowSnackbar(
                        "Internet access restored"
                    )
                )
            } else {
                emitFailure(
                    result,
                    "Unable to restore internet."
                )
            }
        }
    }

    fun renameDevice(
        pdid: String,
        newName: String
    ) {
        val previous =
            state.value.devices
                .find {
                    it.pdid == pdid
                }
                ?.friendlyName
                .orEmpty()

        viewModelScope.launch {
            val result =
                eventRepository.renameDevice(
                    pdid,
                    newName
                )

            if (
                result is ApiResult.Success
            ) {
                _undoState.value =
                    UndoState(
                        "Device renamed"
                    ) {
                        if (
                            previous.isNotBlank()
                        ) {
                            viewModelScope.launch {
                                eventRepository
                                    .renameDevice(
                                        pdid,
                                        previous
                                    )
                            }
                        }
                    }
            } else {
                emitFailure(
                    result,
                    "Unable to rename device."
                )
            }
        }
    }

    suspend fun getDeviceLogs(
        pdid: String
    ): ApiResult<List<FlowLog>> =
        eventRepository.getDeviceLogs(
            pdid
        )

    // ----------------------------------------------------------------
    // Users
    // ----------------------------------------------------------------

    fun assignDeviceUser(
        pdid: String,
        userId: String
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .assignDeviceUser(
                        pdid,
                        userId
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to assign user."
                )
            }
        }
    }

    fun createUser(
        user: User
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .createUser(
                        user
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to create user."
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // Global controls
    // ----------------------------------------------------------------

    fun toggleVacationMode(
        enabled: Boolean
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .toggleVacationMode(
                        enabled
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to change Vacation Mode."
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // Policies
    // ----------------------------------------------------------------

    fun exportPolicies() {
        viewModelScope.launch {
            val result =
                eventRepository
                    .exportPolicies()

            if (
                result is ApiResult.Success
            ) {
                eventRepository.emitUiEvent(
                    UiEvent.ShowSnackbar(
                        "Policies exported"
                    )
                )
            } else {
                emitFailure(
                    result,
                    "Unable to export policies."
                )
            }
        }
    }

    fun importPolicies(
        payload: String
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .importPolicies(
                        payload
                    )

            if (
                result is ApiResult.Success
            ) {
                eventRepository.emitUiEvent(
                    UiEvent.ShowSnackbar(
                        "Policies imported"
                    )
                )
            } else {
                emitFailure(
                    result,
                    "Unable to import policies."
                )
            }
        }
    }

    fun savePolicy(
        policy: Policy
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .savePolicy(
                        policy
                    )

            if (
                result is ApiResult.Success
            ) {
                eventRepository.emitUiEvent(
                    UiEvent.ShowSnackbar(
                        if (
                            policy.id ==
                            "global_default"
                        ) {
                            "Global access updated"
                        } else {
                            "Rule saved"
                        }
                    )
                )
            } else {
                emitFailure(
                    result,
                    "Unable to save rule."
                )
            }
        }
    }

    fun deletePolicy(
        policyId: String,
        policyName: String,
        policy: Policy
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .deletePolicy(
                        policyId
                    )

            if (
                result is ApiResult.Success
            ) {
                _undoState.value =
                    UndoState(
                        "Rule '$policyName' deleted"
                    ) {
                        viewModelScope.launch {
                            eventRepository
                                .savePolicy(
                                    policy
                                )
                        }
                    }
            } else {
                emitFailure(
                    result,
                    "Unable to delete rule."
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // Schedules
    // ----------------------------------------------------------------

    fun saveSchedule(
        schedule: Schedule
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .saveSchedule(
                        schedule
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to save schedule."
                )
            }
        }
    }

    fun deleteSchedule(
        scheduleId: String
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .deleteSchedule(
                        scheduleId
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to delete schedule."
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // Tags
    // ----------------------------------------------------------------

    fun createTag(
        tag: Tag
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .createTag(
                        tag
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to create tag."
                )
            }
        }
    }

    fun updateTag(
        tag: Tag
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .updateTag(
                        tag
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to update tag."
                )
            }
        }
    }

    fun deleteTag(
        tagId: String
    ) {
        viewModelScope.launch {
            val result =
                eventRepository
                    .deleteTag(
                        tagId
                    )

            if (
                result !is ApiResult.Success
            ) {
                emitFailure(
                    result,
                    "Unable to delete tag."
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // Error translation
    // ----------------------------------------------------------------

    private suspend fun emitFailure(
        result: ApiResult<*>,
        fallback: String
    ) {
        val message =
            when (result) {
                is ApiResult.Success<*> ->
                    fallback

                is ApiResult.AuthenticationError ->
                    result.message

                is ApiResult.HttpError ->
                    result.message

                is ApiResult.ConflictError ->
                    result.message

                is ApiResult.NetworkError ->
                    result.cause.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: fallback

                is ApiResult.SerializationError ->
                    "The LIAS server returned an invalid response."
            }

        eventRepository.emitUiEvent(
            UiEvent.ShowSnackbarError(
                message
            )
        )
    }
}
