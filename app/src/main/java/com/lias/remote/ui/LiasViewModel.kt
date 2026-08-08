// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 24.0.0
//
// Purpose:
//   UI orchestration over server-authoritative EventRepository state.
//
// Batch 24:
//   - No client-generated Pause Policy.
//   - Pause calls LIAS dedicated endpoint.
//   - EffectiveStatus is the only pause/extension UI authority.
//   - Adds tag effective-status access used by Batch 19.
//   - Adds refresh() used by modern inventory screens.
//   - Removes emoji status messaging.
//   - Uses Batch 22 error presentation.
//   - Compatibility overload prevents stale callers from silently
//     requesting unsupported custom pause durations.
// ====================================================================

package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.diagnostics.ErrorPresentation
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

    val state:
        StateFlow<UiState> =
        eventRepository.state

    val uiEvents =
        eventRepository.uiEvents

    private val _pendingSecurityAlert =
        MutableStateFlow<
            SecurityAlertPayload?
        >(
            null
        )

    val pendingSecurityAlert:
        StateFlow<
            SecurityAlertPayload?
        > =
        _pendingSecurityAlert
            .asStateFlow()

    private val _undoState =
        MutableStateFlow<
            UndoState?
        >(
            null
        )

    val undoState:
        StateFlow<
            UndoState?
        > =
        _undoState
            .asStateFlow()

    init {

        eventRepository.start()

        viewModelScope.launch {

            eventRepository
                .uiEvents
                .collect {
                    event ->

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
                                    "",
                                timestamp =
                                    Instant.now()
                                        .toString()
                            )
                    }
                }
        }
    }

    fun refresh() {

        viewModelScope.launch {

            eventRepository
                .refreshAll()
        }
    }

    fun dismissSecurityAlert() {

        _pendingSecurityAlert.value =
            null
    }

    fun clearUndo() {

        _undoState.value =
            null
    }

    /**
     * Kept only for the existing diagnostic/demo trigger in the
     * supplied Settings UI.
     */
    fun triggerSecurityAlert() {

        _pendingSecurityAlert.value =
            SecurityAlertPayload(
                alertType =
                    "diagnostic_test",
                details =
                    "Diagnostic security alert test.",
                pdid =
                    state.value
                        .devices
                        .firstOrNull()
                        ?.pdid
                        .orEmpty(),
                timestamp =
                    Instant.now()
                        .toString()
            )
    }

    // ----------------------------------------------------------------
    // Effective status
    // ----------------------------------------------------------------

    /**
     * Non-actionable fallback.
     *
     * Never fabricate "blocked + extend available" while status is
     * still loading.
     */
    fun effectiveStatusFor(
        pdid: String
    ): EffectiveStatus =
        state.value
            .deviceEffectiveStatuses[
                pdid
            ]
            ?: unavailableStatus()

    fun tagEffectiveStatusFor(
        tagId: String
    ): EffectiveStatus =
        state.value
            .tagEffectiveStatuses[
                tagId
            ]
            ?: unavailableStatus()

    private fun unavailableStatus():
        EffectiveStatus =
        EffectiveStatus(
            action =
                "unknown",
            source =
                "unavailable",
            extendAvailable =
                false,
            pauseAvailable =
                false,
            activeExtension =
                null
        )

    // ----------------------------------------------------------------
    // Temporary access
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

            presentResult(
                result,
                successMessage =
                    "Access extended for $minutes minutes"
            )
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

            presentResult(
                result,
                successMessage =
                    "Access extension cancelled"
            )
        }
    }

    fun extendTagAccess(
        tagId: String,
        tagName: String,
        minutes: Int
    ) {

        viewModelScope.launch {

            val result =
                eventRepository
                    .extendTagAccess(
                        tagId,
                        minutes
                    )

            presentResult(
                result,
                successMessage =
                    "$tagName access extended for $minutes minutes"
            )
        }
    }

    fun cancelTagExtension(
        tagId: String
    ) {

        viewModelScope.launch {

            val result =
                eventRepository
                    .cancelTagExtension(
                        tagId
                    )

            presentResult(
                result,
                successMessage =
                    "Group extension cancelled"
            )
        }
    }

    /**
     * LIAS currently implements Pause as a fixed one-hour action.
     */
    fun pauseDeviceInternet(
        pdid: String
    ) {

        viewModelScope.launch {

            val result =
                eventRepository
                    .pauseDeviceInternet(
                        pdid
                    )

            presentResult(
                result,
                successMessage =
                    "Internet paused for 1 hour"
            )
        }
    }

    /**
     * Compatibility bridge for a stale caller from pre-Batch-24 UI.
     *
     * It deliberately rejects unsupported durations instead of
     * silently turning "15 minutes" into the backend's one-hour pause.
     */
    @Deprecated(
        message =
            "LIAS Pause is server-defined as one hour. Use pauseDeviceInternet(pdid)."
    )
    fun pauseDeviceInternet(
        pdid: String,
        minutes: Int
    ) {

        if (
            minutes !=
            60
        ) {

            viewModelScope.launch {

                eventRepository
                    ._uiEvents
                    .emit(
                        UiEvent.ShowSnackbarError(
                            "LIAS currently supports a one-hour Pause only."
                        )
                    )
            }

            return
        }

        pauseDeviceInternet(
            pdid
        )
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

            presentResult(
                result,
                successMessage =
                    "Internet resumed"
            )
        }
    }

    // ----------------------------------------------------------------
    // Device metadata
    // ----------------------------------------------------------------

    fun assignTags(
        pdid: String,
        tagIds: List<String>
    ) {

        val previousTags =
            state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?.safeTags
                ?: listOf(
                    "generic"
                )

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
                        message =
                            "Tags updated",
                        undoAction = {

                            viewModelScope.launch {

                                eventRepository
                                    .assignDeviceTags(
                                        pdid,
                                        previousTags
                                    )
                            }
                        }
                    )

            } else {

                presentResult(
                    result
                )
            }
        }
    }

    fun renameDevice(
        pdid: String,
        newName: String
    ) {

        val previousName =
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
                        message =
                            "Device renamed to '${newName.trim()}'",
                        undoAction = {

                            viewModelScope.launch {

                                eventRepository
                                    .renameDevice(
                                        pdid,
                                        previousName
                                    )
                            }
                        }
                    )

            } else {

                presentResult(
                    result
                )
            }
        }
    }

    fun assignDeviceUser(
        pdid: String,
        userId: String
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .assignDeviceUser(
                        pdid,
                        userId
                    ),
                successMessage =
                    "User assignment updated"
            )
        }
    }

    fun createUser(
        user: User
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .createUser(
                        user
                    ),
                successMessage =
                    "User created"
            )
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

            presentResult(
                result,
                successMessage =
                    if (
                        policy.id ==
                        "global_default"
                    ) {
                        "Global access mode updated"
                    } else {
                        "Rule saved"
                    }
            )
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
                        message =
                            "Rule '$policyName' deleted",
                        undoAction = {

                            viewModelScope.launch {

                                eventRepository
                                    .savePolicy(
                                        policy.copy(
                                            /*
                                             * Restoring a previously
                                             * deleted policy should
                                             * recreate it if LIAS does
                                             * not permit reuse of the
                                             * deleted identifier.
                                             *
                                             * Existing backend currently
                                             * accepts supplied IDs, so
                                             * preserve it here.
                                             */
                                            id =
                                                policy.id
                                        )
                                    )
                            }
                        }
                    )

            } else {

                presentResult(
                    result
                )
            }
        }
    }

    fun exportPolicies() {

        viewModelScope.launch {

            val result =
                eventRepository
                    .exportPolicies()

            presentResult(
                result,
                successMessage =
                    "Policies exported"
            )
        }
    }

    fun importPolicies(
        payload: String
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .importPolicies(
                        payload
                    ),
                successMessage =
                    "Policies imported"
            )
        }
    }

    // ----------------------------------------------------------------
    // Schedules
    // ----------------------------------------------------------------

    fun saveSchedule(
        schedule: Schedule
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .saveSchedule(
                        schedule
                    ),
                successMessage =
                    "Schedule saved"
            )
        }
    }

    fun deleteSchedule(
        scheduleId: String
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .deleteSchedule(
                        scheduleId
                    ),
                successMessage =
                    "Schedule deleted"
            )
        }
    }

    // ----------------------------------------------------------------
    // Tags
    // ----------------------------------------------------------------

    fun createTag(
        tag: Tag
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .createTag(
                        tag
                    ),
                successMessage =
                    "Tag created"
            )
        }
    }

    fun updateTag(
        tag: Tag
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .updateTag(
                        tag
                    ),
                successMessage =
                    "Tag updated"
            )
        }
    }

    fun deleteTag(
        tagId: String
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .deleteTag(
                        tagId
                    ),
                successMessage =
                    "Tag deleted"
            )
        }
    }

    // ----------------------------------------------------------------
    // Global utilities
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

                presentResult(
                    result
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
    // Messaging
    // ----------------------------------------------------------------

    private suspend fun presentResult(
        result: ApiResult<*>,
        successMessage: String? = null
    ) {

        when (
            result
        ) {

            is ApiResult.Success -> {

                successMessage
                    ?.let {
                        message ->

                        eventRepository
                            ._uiEvents
                            .emit(
                                UiEvent.ShowSnackbar(
                                    message
                                )
                            )
                    }
            }

            else -> {

                val message =
                    ErrorPresentation
                        .from(
                            result
                        )
                        .message

                eventRepository
                    ._uiEvents
                    .emit(
                        UiEvent.ShowSnackbarError(
                            message
                        )
                    )
            }
        }
    }
}
