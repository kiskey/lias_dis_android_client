// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/UiState.kt
// Version: 13.0.0
//
// Purpose:
//   Canonical application state.
//
// Batch 13:
//   Adds explicit transport/lifecycle metadata so UI banners do not
//   misrepresent an intentional background disconnect as a server fault.
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

    val devices:
        List<Device> =
        emptyList(),

    val tags:
        List<Tag> =
        emptyList(),

    val policies:
        List<Policy> =
        emptyList(),

    val schedules:
        List<Schedule> =
        emptyList(),

    val stats:
        NetworkStats? =
        null,

    val users:
        List<User> =
        emptyList(),

    val deviceEffectiveStatuses:
        Map<String, EffectiveStatus> =
        emptyMap(),

    val tagEffectiveStatuses:
        Map<String, EffectiveStatus> =
        emptyMap(),

    val connectionState:
        ConnectionState =
        ConnectionState.DISCONNECTED,

    val isNetworkAvailable:
        Boolean =
        true,

    val isAppForeground:
        Boolean =
        false,

    val isInitialLoaded:
        Boolean =
        false,

    val isRefreshing:
        Boolean =
        false,

    /**
     * Wall-clock time of the last successful complete REST
     * reconciliation.
     */
    val lastSuccessfulSyncMs:
        Long =
        0L,

    /**
     * Human-readable transport failure from the SSE layer.
     *
     * This is informational. Cached content remains usable.
     */
    val transportError:
        String? =
        null,

    val errorMessage:
        String? =
        null,

    val syncState:
        SyncState =
        SyncState.Idle
)

sealed interface SyncState {

    data object Idle :
        SyncState

    data object Loading :
        SyncState

    data class Ready(
        val syncedAtMs: Long
    ) : SyncState

    data class Stale(
        val message: String,
        val lastSuccessfulSyncMs:
            Long
    ) : SyncState

    data class Failed(
        val message: String
    ) : SyncState
}

val SyncState.hasUsableData:
    Boolean
    get() =
        when (this) {

            is SyncState.Ready,
            is SyncState.Stale ->
                true

            SyncState.Idle,
            SyncState.Loading,
            is SyncState.Failed ->
                false
        }

sealed class UiEvent {

    data class ShowSnackbar(
        val message: String
    ) : UiEvent()

    data class ShowSnackbarError(
        val message: String
    ) : UiEvent()

    data class ShowSecurityAlert(
        val details: String,
        val pdid: String =
            ""
    ) : UiEvent()
}
