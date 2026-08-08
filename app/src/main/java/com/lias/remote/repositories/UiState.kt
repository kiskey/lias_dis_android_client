// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/UiState.kt
// Version: 5.0.0
//
// Purpose:
//   Single state container consumed by the Android UI.
//
// Architectural rule:
//   ConnectionState describes live transport.
//   SyncState describes REST/cache synchronization.
//   The two must never be conflated.
//
// Existing LIAS domain collections are preserved.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.NetworkStats
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ConnectionState

data class UiState(

    // ---------------------------------------------------------------
    // Domain data
    // ---------------------------------------------------------------

    val devices: List<Device> = emptyList(),

    val tags: List<Tag> = emptyList(),

    val policies: List<Policy> = emptyList(),

    val schedules: List<Schedule> = emptyList(),

    val stats: NetworkStats? = null,

    val users: List<User> = emptyList(),

    // ---------------------------------------------------------------
    // Effective policy state
    // ---------------------------------------------------------------

    val deviceEffectiveStatuses:
        Map<String, EffectiveStatus> =
        emptyMap(),

    val tagEffectiveStatuses:
        Map<String, EffectiveStatus> =
        emptyMap(),

    // ---------------------------------------------------------------
    // Transport state
    // ---------------------------------------------------------------

    val connectionState:
        ConnectionState =
        ConnectionState.DISCONNECTED,

    // ---------------------------------------------------------------
    // REST/data synchronization state
    // ---------------------------------------------------------------

    val syncState:
        SyncState =
        SyncState.Idle,

    /**
     * True once at least one complete primary synchronization has
     * successfully populated the application.
     *
     * Kept as a convenience property for existing UI code.
     */
    val isInitialLoaded: Boolean = false,

    /**
     * Last successful primary synchronization time.
     *
     * Epoch milliseconds.
     */
    val lastSuccessfulSyncAt:
        Long? = null,

    /**
     * Human-readable synchronization error.
     *
     * This is intentionally separate from transient snackbar events.
     */
    val errorMessage:
        String? = null
)

sealed class UiEvent {

    data class ShowSnackbar(
        val message: String
    ) : UiEvent()

    data class ShowSnackbarError(
        val message: String
    ) : UiEvent()

    data class ShowSecurityAlert(
        val details: String
    ) : UiEvent()
}
