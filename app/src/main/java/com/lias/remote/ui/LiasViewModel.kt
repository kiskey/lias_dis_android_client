package com.lias.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lias.remote.core.diagnostics.ErrorPresentation
import com.lias.remote.core.models.Conflict
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
import com.lias.remote.repositories.cancelDeviceExtensionAuthoritatively
import com.lias.remote.repositories.cancelTagExtensionAuthoritatively
import com.lias.remote.repositories.createTag
import com.lias.remote.repositories.createUser
import com.lias.remote.repositories.deletePolicy
import com.lias.remote.repositories.deleteSchedule
import com.lias.remote.repositories.deleteTag
import com.lias.remote.repositories.exportPolicies
import com.lias.remote.repositories.extendDeviceAuthoritatively
import com.lias.remote.repositories.extendTagAuthoritatively
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

/**
 * UI orchestration over server-authoritative EventRepository state.
 *
 * This ViewModel does not construct enforcement rules or infer access
 * state from policy IDs.
 */
class LiasViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    val state: StateFlow<UiState> =
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
        StateFlow<SecurityAlertPayload?> =
        _pendingSecurityAlert
            .asStateFlow()

    private val _undoState =
        MutableStateFlow<
            UndoState?
        >(
            null
        )

    val undoState:
        StateFlow<UndoState?> =
        _undoState
            .asStateFlow()

    init {

        eventRepository.start()

        viewModelScope.launch {

            eventRepository.uiEvents
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

            eventRepository.refreshAll()
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
    // EffectiveStatus
    // ----------------------------------------------------------------

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

            presentResult(
                eventRepository
                    .extendDeviceAuthoritatively(
                        pdid,
                        minutes
                    ),
                "Access extended for $minutes minutes"
            )
        }
    }

    fun cancelDeviceExtension(
        pdid: String
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .cancelDeviceExtensionAuthoritatively(
                        pdid
                    ),
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

            presentResult(
                eventRepository
                    .extendTagAuthoritatively(
                        tagId,
                        minutes
                    ),
                "$tagName access extended for $minutes minutes"
            )
        }
    }

    fun cancelTagExtension(
        tagId: String
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .cancelTagExtensionAuthoritatively(
                        tagId
                    ),
                "Group extension cancelled"
            )
        }
    }

    /**
     * LIAS currently owns Pause duration as one hour.
     */
    fun pauseDeviceInternet(
        pdid: String
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .pauseDeviceAuthoritatively(
                        pdid
                    ),
                "Internet paused for 1 hour"
            )
        }
    }

    fun unpauseDeviceInternet(
        pdid: String
    ) {

        viewModelScope.launch {

            presentResult(
                eventRepository
                    .resumeDeviceAuthoritatively(
                        pdid
                    ),
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
                        action = {

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
                        action = {

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
                "User created"
            )
        }
    }

    suspend fun validatePolicy(
        scheduleIds: List<String>
    ): ApiResult<List<Conflict>> =
        eventRepository.validatePolicy(
            scheduleIds
        )

    // ----------------------------------------------------------------
    // Policies
    // ----------------------------------------------------------------

    suspend fun savePolicyAwait(
        policy: Policy
    ): ApiResult<Policy> {

        val result =
            eventRepository
                .savePolicy(
                    policy
                )

        presentResult(
            result,
            if (
                policy.id ==
                "global_default"
            ) {
                "Global access mode updated"
            } else {
                "Rule saved"
            }
        )

        return result
    }

    fun savePolicy(
        policy: Policy
    ) {

        viewModelScope.launch {

            savePolicyAwait(
                policy
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
                    null

                eventRepository
                    ._uiEvents
                    .emit(
                        UiEvent.ShowSnackbar(
                            "Rule '$policyName' deleted"
                        )
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

            presentResult(
                eventRepository
                    .exportPolicies(),
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
                "Tag deleted"
            )
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

            /*
             * GlobalControlMutations emits its own authoritative
             * success message because it knows the resulting global
             * access semantics.
             */
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
    // Messages
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
