// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/UiState.kt
// Version: 1.0.0
// Purpose: Global UI State representation. Combines all REST models
//          and SSE connection status into a single immutable snapshot
//          for declarative Compose rendering.
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
