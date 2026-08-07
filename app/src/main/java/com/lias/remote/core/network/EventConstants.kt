// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/EventConstants.kt
// Version: 1.3.0
// Purpose: SSE Wire Event constants aligned with DIS/LIAS backend.
// ====================================================================

package com.lias.remote.core.network

object EventConstants {
    const val DEVICE_ADDED = "device.added"
    const val DEVICE_REMOVED = "device.removed"
    const val DEVICE_ONLINE = "device.online"
    const val DEVICE_OFFLINE = "device.offline"
    const val HOSTNAME_CHANGED = "device.hostname_changed"
    const val FINGERPRINT_UPDATED = "device.fingerprint_updated"
    const val IP_CHANGED = "device.ip_changed"
    const val MAC_CHANGED = "device.mac_changed"
    const val DEVICE_REIDENTIFIED = "device.reidentified"
    const val SECURITY_ALERT = "security.alert"
    const val EFFECTIVE_STATUS_CHANGED = "effective.status_changed"
    const val PING = "ping"
}
