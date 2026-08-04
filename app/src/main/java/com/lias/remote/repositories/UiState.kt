// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/UiState.kt
// Version: 1.1.0
// Audit Fixes: 
//   1. Added ShowSnackbarError event for save failures (GAP-U43).
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ConnectionState

data class UiState(
    val devices: List<Device> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val policies: List<Policy> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isInitialLoaded: Boolean = false,
    val errorMessage: String? = null
)

// Defines transient UI events for Snackbars
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    
    // GAP-U43 Fix: Dedicated error event for save failures
    data class ShowSnackbarError(val message: String) : UiEvent()
}
