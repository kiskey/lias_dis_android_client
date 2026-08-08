// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/LiasViewModel.kt
// Version: 5.0.0
//
// Purpose:
//   Lifecycle-aware UI façade over EventRepository.
//
// Changes:
//   - Repository remains the source of truth.
//   - UI receives a stable StateFlow.
//   - Repository startup is idempotent.
//   - Security alerts remain transient UI state.
//   - Existing action APIs remain intact.
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
import com.lias.remote.repositories.*
import com.lias.remote.ui.components.UndoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class LiasViewModel(
    private val eventRepository:
        EventRepository
) : ViewModel() {

    // ----------------------------------------------------------------
    // Primary application state
    // ----------------------------------------------------------------

    val state:
        StateFlow<UiState> =
        eventRepository.state

    val uiEvents =
        eventRepository.uiEvents

    // ----------------------------------------------------------------
    // Transient security presentation
    // ----------------------------------------------------------------

    private val _pendingSecurityAlert =
        MutableStateFlow<
            SecurityAlertPayload?
        >(null)

    val pendingSecurityAlert:
        StateFlow<
            SecurityAlertPayload?
        > =
        _pendingSecurityAlert
            .asStateFlow()

    // ----------------------------------------------------------------
    // Undo presentation
    // ----------------------------------------------------------------

    private val _undoState =
        MutableStateFlow<
            UndoState?
        >(null)

    val undoState:
        StateFlow<
            UndoState?
        > =
        _undoState.asStateFlow()

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

                                pdid = "",

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
    // Security alert
    // ----------------------------------------------------------------

    fun triggerSecurityAlert() {

        _pendingSecurityAlert.value =
            SecurityAlertPayload(
                alertType =
                    "mac_spoof_detected",

                details =
                    "Potential MAC spoofing detected via network monitoring.",

                pdid =
                    state.value
                        .devices
                        .firstOrNull()
                        ?.pdid
                        ?: "",

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
                result is
                    ApiResult.Success
            ) {

                eventRepository
                    ._uiEvents
                    .emit(
                        UiEvent.ShowSnackbar(
                            "Access extended by $minutes minutes"
                        )
                    )
            }
        }
    }

    // ----------------------------------------------------------------
    // Effective status helpers
    // ----------------------------------------------------------------

    fun effectiveStatusFor(
        pdid: String
    ): EffectiveStatus {

        return state.value
            .deviceEffectiveStatuses[
                pdid
            ]
            ?: EffectiveStatus()
    }

    fun effectiveStatusForTag(
        tagId: String
    ): EffectiveStatus {

        return state.value
            .tagEffectiveStatuses[
                tagId
            ]
            ?: EffectiveStatus()
    }

    // ----------------------------------------------------------------
    // Convenience refresh
    // ----------------------------------------------------------------

    fun refresh() {

        viewModelScope.launch {
            eventRepository.refreshAll()
        }
    }

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    override fun onCleared() {

        /*
         * EventRepository intentionally owns the application-scoped
         * real-time stream and is shared through AppContainer.
         *
         * The ViewModel therefore does not disconnect it here.
         */
        super.onCleared()
    }
}
