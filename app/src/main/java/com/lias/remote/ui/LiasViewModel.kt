// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 16.0.0
//
// Purpose:
//   UI-facing LIAS façade.
//
// Batch 16 safety changes:
//   - Removed unsafe Undo for policy deletion.
//   - Removed unsafe Undo for remote rename.
//   - Removed unsafe Undo for tag assignment.
//   - Delete operations explicitly report success/failure.
//   - All server mutations continue through authoritative repositories.
//
// Rationale:
//   A cached object's inverse is not necessarily the inverse of the
//   CURRENT server state five seconds later.
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
import com.lias.remote.repositories.cancelTagExtensionAuthoritatively
import com.lias.remote.repositories.createTag
import com.lias.remote.repositories.createUser
import com.lias.remote.repositories.deletePolicy
import com.lias.remote.repositories.deleteSchedule
import com.lias.remote.repositories.deleteTag
import com.lias.remote.repositories.extendDeviceAuthoritatively
import com.lias.remote.repositories.extendTagAuthoritatively
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

    /*
     * Retained because LiasNavHost already owns the UndoToast.
     *
     * Batch 16 deliberately does NOT populate it for remote LIAS
     * mutations. It remains available for future local-only operations.
     */
    private val _undoState =
        MutableStateFlow<
            UndoState?
        >(null)

    val undoState:
        StateFlow<UndoState?> =
        _undoState
            .asStateFlow()

    init {

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
    // Sync
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
    // Temporary access
    // ----------------------------------------------------------------

    fun extendDeviceAccess(
        pdid: String,
        minutes: Int
    ) {

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .extendDeviceAuthoritatively(
                            pdid,
                            minutes
                        )
            ) {

                is ApiResult.Success -> {

                    val actualMinutes =
                        result.data.minutes
                            .takeIf {
                                it > 0
                            }
                            ?: minutes

                    emitMessage(
                        "Access extended for $actualMinutes minutes"
                    )
                }

                else ->
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

            when (
                val result =
                    eventRepository
                        .cancelDeviceExtensionAuthoritatively(
                            pdid
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Access extension cancelled"
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to cancel extension."
                    )
            }
        }
    }

    fun extendTagAccess(
        tagId: String,
        tagName: String,
        minutes: Int
    ) {

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .extendTagAuthoritatively(
                            tagId,
                            minutes
                        )
            ) {

                is ApiResult.Success -> {

                    val actualMinutes =
                        result.data.minutes
                            .takeIf {
                                it > 0
                            }
                            ?: minutes

                    emitMessage(
                        "Access extended for $tagName · $actualMinutes minutes"
                    )
                }

                else ->
                    emitFailure(
                        result,
                        "Unable to extend access for $tagName."
                    )
            }
        }
    }

    fun cancelTagExtension(
        tagId: String
    ) {

        val tagName =
            state.value.tags
                .find {
                    it.id ==
                        tagId
                }
                ?.name
                ?: "tag"

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .cancelTagExtensionAuthoritatively(
                            tagId
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Extension cancelled for $tagName"
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to cancel tag extension."
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

        if (
            minutes !=
            FIXED_PAUSE_MINUTES
        ) {

            viewModelScope.launch {
                emitError(
                    "LIAS Pause Internet is fixed to 1 hour."
                )
            }

            return
        }

        val currentStatus =
            effectiveStatusFor(
                pdid
            )

        if (
            currentStatus == null
        ) {

            viewModelScope.launch {
                emitError(
                    "Waiting for the current device access state."
                )
            }

            return
        }

        if (
            !currentStatus.pauseAvailable
        ) {

            viewModelScope.launch {

                emitError(
                    when {

                        currentStatus.source ==
                            "infrastructure" ->
                            "Infrastructure devices are always online."

                        currentStatus.source ==
                            "global" &&
                            currentStatus.action ==
                                "block" ->
                            "Global Access is already blocking this device."

                        else ->
                            "Pause is not available for the current access state."
                    }
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

                is ApiResult.Success ->
                    emitMessage(
                        "Internet paused for 1 hour"
                    )

                else ->
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

            when (
                val result =
                    eventRepository
                        .resumeDeviceAuthoritatively(
                            pdid
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Internet access resumed"
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to resume internet."
                    )
            }
        }
    }

    // ----------------------------------------------------------------
    // Device metadata
    // ----------------------------------------------------------------

    fun assignTags(
        pdid: String,
        tagIds: List<String>
    ) {

        /*
         * No UndoState here.
         *
         * Replaying the old tag list later can remove tags added by a
         * different client after this mutation completed.
         */
        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .assignDeviceTags(
                            pdid,
                            tagIds
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Device tags updated"
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to update tags."
                    )
            }
        }
    }

    fun renameDevice(
        pdid: String,
        newName: String
    ) {

        /*
         * No stale-name Undo.
         *
         * A second client may rename this device immediately after this
         * request. Replaying the cached previous name would overwrite
         * that newer authoritative mutation.
         */
        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .renameDevice(
                            pdid,
                            newName
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Device renamed"
                    )

                else ->
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

            when (
                val result =
                    eventRepository
                        .assignDeviceUser(
                            pdid,
                            userId
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Assigned user updated"
                    )

                else ->
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

            when (
                val result =
                    eventRepository
                        .createUser(
                            user
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "User created"
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to create user."
                    )
            }
        }
    }

    // ----------------------------------------------------------------
    // Global / Vacation
    // ----------------------------------------------------------------

    fun toggleVacationMode(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .toggleVacationMode(
                            enabled
                        )
            ) {

                is ApiResult.Success ->
                    Unit

                else ->
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

    fun savePolicy(
        policy: Policy
    ) {

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .savePolicy(
                            policy
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        if (
                            result.data.id ==
                            "global_default"
                        ) {
                            "Global access updated"
                        } else {
                            "Rule saved"
                        }
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to save rule."
                    )
            }
        }
    }

    /**
     * Compatibility signature retained for existing RulesScreen calls.
     *
     * The cached Policy parameter is intentionally ignored.
     *
     * DO NOT restore it as Undo.
     */
    fun deletePolicy(
        policyId: String,
        policyName: String,
        @Suppress("UNUSED_PARAMETER")
        policy: Policy
    ) {

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .deletePolicy(
                            policyId
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Rule '$policyName' deleted"
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to delete rule."
                    )
            }
        }
    }

    // ----------------------------------------------------------------
    // Policy import/export
    // ----------------------------------------------------------------

    fun exportPolicies() {

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .exportPolicies()
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Policy export prepared"
                    )

                else ->
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

            when (
                val result =
                    eventRepository
                        .importPolicies(
                            payload
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Policies imported"
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to import policies."
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

            when (
                val result =
                    eventRepository
                        .saveSchedule(
                            schedule
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Schedule saved"
                    )

                else ->
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

            when (
                val result =
                    eventRepository
                        .deleteSchedule(
                            scheduleId
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Schedule deleted"
                    )

                else ->
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

            when (
                val result =
                    eventRepository
                        .createTag(
                            tag
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Tag created"
                    )

                else ->
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

            when (
                val result =
                    eventRepository
                        .updateTag(
                            tag
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "Tag updated"
                    )

                else ->
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

        val tagName =
            state.value.tags
                .find {
                    it.id ==
                        tagId
                }
                ?.name
                ?: "Tag"

        viewModelScope.launch {

            when (
                val result =
                    eventRepository
                        .deleteTag(
                            tagId
                        )
            ) {

                is ApiResult.Success ->
                    emitMessage(
                        "$tagName deleted"
                    )

                else ->
                    emitFailure(
                        result,
                        "Unable to delete tag."
                    )
            }
        }
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
    // Messaging
    // ----------------------------------------------------------------

    private suspend fun emitMessage(
        message: String
    ) {

        eventRepository
            .emitUiEvent(
                UiEvent.ShowSnackbar(
                    message
                )
            )
    }

    private suspend fun emitError(
        message: String
    ) {

        eventRepository
            .emitUiEvent(
                UiEvent.ShowSnackbarError(
                    message
                )
            )
    }

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

        emitError(
            message
        )
    }
}
