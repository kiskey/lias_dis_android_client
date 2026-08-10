// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/EventConstants.kt
// Version: 2.0.0
//
// Purpose:
//   Canonical LIAS/DIS SSE event names.
//
// Contract:
//   These values intentionally mirror the event constants in the
//   supplied LIAS/DIS shared event model.
// ====================================================================

package com.lias.remote.core.network

object EventConstants {

    const val DEVICE_ADDED =
        "device.added"

    const val DEVICE_REMOVED =
        "device.removed"

    const val DEVICE_ONLINE =
        "device.online"

    const val DEVICE_OFFLINE =
        "device.offline"

    const val HOSTNAME_CHANGED =
        "device.hostname_changed"

    const val FINGERPRINT_UPDATED =
        "device.fingerprint_updated"

    const val IP_CHANGED =
        "device.ip_changed"

    const val MAC_CHANGED =
        "device.mac_changed"

    const val DEVICE_REIDENTIFIED =
        "device.reidentified"

    const val SECURITY_ALERT =
        "security.alert"

    const val EFFECTIVE_STATUS_CHANGED =
        "effective.status_changed"

    const val IDENTITY_CANDIDATE_CHANGED =
        "identity.candidate.changed"

    const val IDENTITY_CANDIDATE_DECIDED =
        "identity.candidate.decided"

    const val IDENTITY_BINDING_CHANGED =
        "identity.binding.changed"

    const val PING =
        "ping"

    /**
     * Returns true when the event is associated with a device identity.
     *
     * This is deliberately limited to the canonical device events
     * rather than using a broad "device." prefix, so newly introduced
     * backend events cannot accidentally be interpreted as supported
     * client events.
     */
    fun isDeviceEvent(type: String): Boolean =
        when (type) {
            DEVICE_ADDED,
            DEVICE_REMOVED,
            DEVICE_ONLINE,
            DEVICE_OFFLINE,
            HOSTNAME_CHANGED,
            FINGERPRINT_UPDATED,
            IP_CHANGED,
            MAC_CHANGED,
            DEVICE_REIDENTIFIED -> true

            else -> false
        }

    /**
     * Returns true for events that can affect the displayed effective
     * access state.
     */
    fun affectsEffectiveStatus(type: String): Boolean =
        type == EFFECTIVE_STATUS_CHANGED

    fun affectsIdentity(type: String): Boolean =
        when (type) {
            IDENTITY_CANDIDATE_CHANGED,
            IDENTITY_CANDIDATE_DECIDED,
            IDENTITY_BINDING_CHANGED,
            DEVICE_REIDENTIFIED -> true

            else -> false
        }
}
