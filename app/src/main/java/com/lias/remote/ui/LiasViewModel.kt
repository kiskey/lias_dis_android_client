// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 10.0.0
//
// Purpose:
//   Complete UI-facing façade for LIAS Remote.
//
// Batch 10:
//   - Pause goes through LIAS /pause endpoint.
//   - Android no longer fabricates pause Policy/Schedule objects.
//   - Pause duration is fixed to the server-supported 60 minutes.
//   - Extend returns/uses authoritative server duration.
//   - Temporary status is re-fetched through EventRepository.
//   - Existing policy/schedule/device/tag/user functionality retained.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.SecurityAlertPayload
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.FIXED_PAUSE_MINUTES
import com.lias.remote.repositories.EventRepository
import com.lias.remote.repositories.UiEvent
import com.lias.remote.repositories.UiState
import com.lias.remote.repositories.assignDeviceTags
import com.lias.remote.repositories.assignDeviceUser
import com.lias.remote.repositories.cancelDeviceExtensionAuthoritatively
import com.lias.remote.repositories.createTag
import com.lias.remote.repositories.createUser
import com.lias.remote.repositories.deletePolicy
import com.lias.remote.repositories.deleteSchedule
import com.lias.remote.repositories.deleteTag
import com.lias.remote.repositories.extendDeviceAuthoritatively
import com.lias.remote.repositories.exportPolicies
import com.lias.remote.repositories.getDeviceLogs
import com.lias.remote.repositories.importPolicies
import com.lias.remote.repositories.pauseDeviceAuthoritatively
import com.lias.remote.repositories.renameDevice
import com.lias.remote.repositories.resumeDeviceAuthoritatively
import com.lias.remote.repositories.savePolicy
import com.lias.remote.repositories.saveSchedule
import com.lias.remote.repositories.toggleVacationMode
import com.lias.remote.repositories.updateTag
import com.lias.remote.repositories.validatePolicy
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

    val state:
        StateFlow<UiState> =
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
        _undoState
            .asStateFlow()

    init {

        eventRepository.start()

        viewModelScope.launch {

            eventRepository
                .uiEvents
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
                                    Instant
                                        .now()
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
            eventRepository
                .refreshAll()
        }
    }

    fun clearError() {

        eventRepository
            .clearError()
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
                    state.value
                        .devices
                        .firstOrNull()
                        ?.pdid
                        .orEmpty(),
                timestamp =
                    Instant
                        .now()
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
    // Effective status
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
    // Extend Access
    // ----------------------------------------------------------------

    fun extendDeviceAccess(
        pdid: String,
        minutes: Int
    ) {

        viewModelScope.launch {

            val result =
                eventRepository
                    .extendDeviceAuthoritatively(
                        pdid =
                            pdid,
                        minutes =
                            minutes
                    )

            when (result) {

                is ApiResult.Success -> {

                    val actualMinutes =
                        result.data
                            .minutes
                            .takeIf {
                                it > 0
                            }
                            ?: minutes

                    eventRepository
                        .emitUiEvent(
                            UiEvent.ShowSnackbar(
                                "Access extended for $actualMinutes minutes"
                            )
                        )
                }

                else -> {
                    emitFailure(
                        result,
                        "Unable to extend access."
                    )
                }
            }
        }
    }

    fun cancelDeviceExtension(
        pdid: String
    ) {

        viewModelScope.launch {

            val result =
                eventRepository
                    .cancelDeviceExtensionAuthoritatively(
                        pdid
                    )

            if (
                result is
                ApiResult.Success
            ) {

                eventRepository
                    .emitUiEvent(
                        UiEvent.ShowSnackbar(
                            "Access extension cancelled"
                        )
                    )

            } else {

                emitFailure(
                    result,
                    "Unable to cancel extension."
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // Pause / Resume
    // ----------------------------------------------------------------

    fun pauseDeviceInternet(
        pdid: String,
        minutes: Int =
            FIXED_PAUSE_MINUTES
    ) {

        /*
         * Keep the argument for source compatibility with older UI
         * call sites, but refuse to imply unsupported durations.
         */
        if (
            minutes !=
            FIXED_PAUSE_MINUTES
        ) {

            viewModelScope.launch {

                eventRepository
                    .emitUiEvent(
                        UiEvent.ShowSnackbarError(
                            "LIAS Pause Internet is fixed to 1 hour."
                        )
                    )
            }

            return
        }

        val currentStatus =
            effectiveStatusFor(
                pdid
            )

        if (
            currentStatus != null &&
            !currentStatus.pauseAvailable
        ) {

            viewModelScope.launch {

                eventRepository
                    .emitUiEvent(
                        UiEvent.ShowSnackbarError(
                            pauseUnavailableMessage(
                                currentStatus
                            )
                        )
                    )
            }

            return
        }

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .pauseDeviceAuthoritatively(
                            pdid
                        )
            ) {

                is ApiResult.Success -> {

                    eventRepository
                        .emitUiEvent(
                            UiEvent.ShowSnackbar(
                                "Internet paused for 1 hour"
                            )
                        )
                }

                else -> {

                    emitFailure(
                        result,
                        "Unable to pause internet."
                    )
                }
            }
        }
    }

    fun unpauseDeviceInternet(
        pdid: String
    ) {

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .resumeDeviceAuthoritatively(
                            pdid
                        )
            ) {

                is ApiResult.Success -> {

                    eventRepository
                        .emitUiEvent(
                            UiEvent.ShowSnackbar(
                                "Internet access resumed"
                            )
                        )
                }

                else -> {

                    emitFailure(
                        result,
                        "Unable to resume internet."
                    )
                }
            }
        }
    }

    private fun pauseUnavailableMessage(
        status: EffectiveStatus
    ): String =
        when (
            status.source
                .lowercase()
        ) {

            "infrastructure" ->
                "Infrastructure devices are always online."

            "global" ->
                if (
                    status.action ==
                    "block"
                ) {
                    "Global Access is already blocking this device."
                } else {
                    "Pause is not available for the current global state."
                }

            else ->
                "Pause is not available for the current device state."
        }

    // ----------------------------------------------------------------
    // Tags assigned to devices
    // ----------------------------------------------------------------

    fun assignTags(
        pdid: String,
        tagIds: List<String>
    ) {

        val previous =
            state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?.safeTags
                .orEmpty()

        viewModelScope.launch {

            val result =
                eventRepository
                    .assignDeviceTags(
                        pdid,
                        tagIds
                    )

            if (
                result is
                ApiResult.Success
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

    // ----------------------------------------------------------------
    // Rename
    // ----------------------------------------------------------------

    fun renameDevice(
        pdid: String,
        newName: String
    ) {

        val previous =
            state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?.friendlyName
                .orEmpty()

        viewModelScope.launch {

            val result =
                eventRepository
                    .renameDevice(
                        pdid,
                        newName
                    )

            if (
                result is
                ApiResult.Success
            ) {

                _undoState.value =
                    UndoState(
                        "Device renamed"
                    ) {

                        if (
                            previous
                                .isNotBlank()
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
        eventRepository
            .getDeviceLogs(
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
                result !is
                ApiResult.Success
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
                result !is
                ApiResult.Success
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
                result !is
                ApiResult.Success
            ) {

                emitFailure(
                    result,
                    "Unable to change Vacation Mode."
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // Policy validation
    // ----------------------------------------------------------------

    suspend fun validatePolicy(
        scheduleIds: List<String>
    ): ApiResult<List<Conflict>> =
        eventRepository
            .validatePolicy(
                scheduleIds
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()
            )

    // ----------------------------------------------------------------
    // Policy import/export
    // ----------------------------------------------------------------

    fun exportPolicies() {

        viewModelScope.launch {

            val result =
                eventRepository
                    .exportPolicies()

            if (
                result is
                ApiResult.Success
            ) {

                eventRepository
                    .emitUiEvent(
                        UiEvent.ShowSnackbar(
                            "Policy export prepared"
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
                result is
                ApiResult.Success
            ) {

                eventRepository
                    .emitUiEvent(
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

    // ----------------------------------------------------------------
    // Policies
    // ----------------------------------------------------------------

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
                result is
                ApiResult.Success
            ) {

                eventRepository
                    .emitUiEvent(
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
                result is
                ApiResult.Success
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
                result is
                ApiResult.Success
            ) {

                eventRepository
                    .emitUiEvent(
                        UiEvent.ShowSnackbar(
                            "Schedule saved"
                        )
                    )

            } else {

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
                result is
                ApiResult.Success
            ) {

                eventRepository
                    .emitUiEvent(
                        UiEvent.ShowSnackbar(
                            "Schedule deleted"
                        )
                    )

            } else {

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
                result !is
                ApiResult.Success
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
                result !is
                ApiResult.Success
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
                result !is
                ApiResult.Success
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
                    result.cause
                        .message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: fallback

                is ApiResult.SerializationError ->
                    "The LIAS server returned an invalid response."
            }

        eventRepository
            .emitUiEvent(
                UiEvent.ShowSnackbarError(
                    message
                )
            )
    }
}
