// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/UiState.kt
// Version: 1.3.0
// Purpose: State container holding models and effective statuses.
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
    val devices: List<Device> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val policies: List<Policy> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val stats: NetworkStats? = null,
    val users: List<User> = emptyList(),
    val deviceEffectiveStatuses: Map<String, EffectiveStatus> = emptyMap(),
    val tagEffectiveStatuses: Map<String, EffectiveStatus> = emptyMap(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isInitialLoaded: Boolean = false,
    val errorMessage: String? = null
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ShowSnackbarError(val message: String) : UiEvent()
    data class ShowSecurityAlert(val details: String) : UiEvent()
}
