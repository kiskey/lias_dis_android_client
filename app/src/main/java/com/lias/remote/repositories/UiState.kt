// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/UiState.kt
// Version: 2.0.0
//
// Purpose:
//   Immutable application state consumed by Compose UI.
//
// Design:
//   State explicitly distinguishes:
//     - first-load state
//     - refresh state
//     - connection state
//     - recoverable error state
//     - effective access state
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

    val deviceEffectiveStatuses:
        Map<String, EffectiveStatus> = emptyMap(),

    val tagEffectiveStatuses:
        Map<String, EffectiveStatus> = emptyMap(),

    val connectionState:
        ConnectionState = ConnectionState.DISCONNECTED,

    val isInitialLoaded: Boolean = false,

    val isRefreshing: Boolean = false,

    val errorMessage: String? = null,

    val lastConnectionError: String? = null
)

sealed class UiEvent {

    data class ShowSnackbar(
        val message: String
    ) : UiEvent()

    data class ShowSnackbarError(
        val message: String
    ) : UiEvent()

    data class ShowSecurityAlert(
        val details: String,
        val pdid: String = ""
    ) : UiEvent()
}
