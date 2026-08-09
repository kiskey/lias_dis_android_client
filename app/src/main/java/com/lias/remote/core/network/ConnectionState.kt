// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ConnectionState.kt
// Version: 5.0.0
//
// Purpose:
//   Represents the lifecycle of the live SSE transport.
//
// Important:
//   This is deliberately NOT the same thing as data synchronization.
//
//   CONNECTED means:
//       The live event stream is established.
//
//   It does NOT necessarily mean:
//       The REST cache has successfully loaded.
//
//   UiState.syncState handles that second concern.
// ====================================================================

package com.lias.remote.core.network

enum class ConnectionState {

    /**
     * No live SSE connection exists.
     */
    DISCONNECTED,

    /**
     * First connection attempt is underway.
     */
    CONNECTING,

    /**
     * SSE stream is actively connected.
     */
    CONNECTED,

    /**
     * SSE connection was lost and the client is retrying.
     */
    RECONNECTING
}
