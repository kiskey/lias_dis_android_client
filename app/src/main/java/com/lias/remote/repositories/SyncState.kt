// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/SyncState.kt
// Version: 5.0.0
//
// Purpose:
//   Represents REST/data synchronization independently from SSE.
//
// This prevents the UI from treating:
//   - "SSE connected"
//   - "REST data loaded"
//   - "data is stale"
//   - "initial load failed"
// as the same condition.
// ====================================================================

package com.lias.remote.repositories

sealed interface SyncState {

    /**
     * No server data has been loaded in this process yet.
     */
    data object Idle : SyncState

    /**
     * Initial synchronization is underway.
     */
    data object Loading : SyncState

    /**
     * Initial or subsequent synchronization completed successfully.
     */
    data class Ready(
        val synchronizedAt: Long
    ) : SyncState

    /**
     * A later synchronization failed while usable cached data exists.
     *
     * The application should continue showing the cached data and
     * communicate that it may be stale.
     */
    data class Stale(
        val synchronizedAt: Long?,
        val message: String
    ) : SyncState

    /**
     * No usable synchronized data exists because the initial load failed.
     */
    data class Failed(
        val message: String
    ) : SyncState
}

val SyncState.hasUsableData: Boolean
    get() =
        when (this) {
            is SyncState.Ready -> true
            is SyncState.Stale -> synchronizedAt != null
            else -> false
        }

val SyncState.isLoading: Boolean
    get() =
        this is SyncState.Loading
