// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ConnectionState.kt
// Version: 1.0.0
// Purpose: Represents the lifecycle states of the SSE connection
//          for UI feedback (e.g., "Reconnecting..." banners).
// ====================================================================

package com.lias.remote.core.network

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}
