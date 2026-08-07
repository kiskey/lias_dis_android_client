// ====================================================================
// File: app/src/main/java/com/lias/remote/core/models/Models.kt
// Version: 2.0.0
// Purpose: Canonical data models for LIAS Remote Client.
// Audit Fix: Removed invalid '#' comments causing top-level syntax errors.
// ====================================================================

package com.lias.remote.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Device(
    val pdid: String = "",
    @SerialName("identity_tier") val identityTier: String = "tentative",
    @SerialName("identity_anchor") val identityAnchor: String = "",
    @SerialName("canonical_hostname") val canonicalHostname: String = "",
    @SerialName("current_mac") val currentMAC: String = "",
    val macs: List<String>? = emptyList(),
    @SerialName("current_ip") val currentIP: String = "",
    val ips: List<String>? = emptyList(),
    val hostname: String = "",
    @SerialName("friendly_name") val friendlyName: String = "",
    val manufacturer: String = "",
    val vendor: String = "",
    val model: String = "",
    @SerialName("device_type") val deviceType: String = "",
    val services: List<String>? = emptyList(),
    val online: Boolean = false,
    @SerialName("first_seen") val firstSeen: String = "",
    @SerialName("last_seen") val lastSeen: String = "",
    val confidence: Double = 0.0,
    val tags: List<String>? = emptyList(),
    @SerialName("user_id") val userID: String? = null
) {
    val safeMacs: List<String> get() = macs ?: emptyList()
    val safeIps: List<String> get() = ips ?: emptyList()
    val safeServices: List<String> get() = services ?: emptyList()
    val safeTags: List<String> get() = tags ?: emptyList()

    val displayName: String
        get() {
            if (friendlyName.isNotBlank()) return friendlyName
            if (hostname.isNotBlank()) return hostname
            if (canonicalHostname.isNotBlank()) return canonicalHostname
            val vm = (vendor + " " + model).trim()
            if (vm.isNotBlank()) return vm
            if (manufacturer.isNotBlank()) return manufacturer
            if (currentMAC.isNotBlank()) return currentMAC
            if (pdid.isNotBlank()) return pdid
            return "Unknown Device"
        }
}

@Serializable
data class ExtensionInfo(
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("minutes_left") val minutesLeft: Int = 0,
    @SerialName("reason_tag") val reasonTag: String = ""
)

@Serializable
data class EffectiveStatus(
    val action: String = "allow",
    val source: String = "global",
    @SerialName("extend_available") val extendAvailable: Boolean = false,
    @SerialName("pause_available") val pauseAvailable: Boolean = false,
    @SerialName("active_extension") val activeExtension: ExtensionInfo? = null
)

@Serializable
data class Tag(
    val id: String,
    val name: String,
    val color: String,
    val precedence: Int,
    val builtin: Boolean
)

@Serializable
data class Policy(
    val id: String,
    val name: String,
    val type: String,
    @SerialName("target_id") val targetID: String = "",
    val action: String,
    @SerialName("schedule_ids") val scheduleIDs: List<String>? = emptyList(),
    @SerialName("schedule_id") val scheduleID: String? = null,
    val priority: Int = 50,
    val enabled: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("reason_tag") val reasonTag: String? = null
) {
    val safeScheduleIDs: List<String> get() = scheduleIDs ?: emptyList()

    fun resolveScheduleIDs(): List<String> {
        val ids = safeScheduleIDs
        if (ids.isNotEmpty()) return ids
        if (!scheduleID.isNullOrEmpty()) return listOf(scheduleID)
        return emptyList()
    }
}

@Serializable
data class Schedule(
    val id: String,
    val name: String,
    val mode: String = "downtime",
    val timezone: String = "UTC",
    val rules: List<ScheduleRule>? = emptyList()
) {
    val safeRules: List<ScheduleRule> get() = rules ?: emptyList()
}

@Serializable
data class ScheduleRule(
    val days: List<String>? = emptyList(),
    @SerialName("start_time") val startTime: String = "00:00",
    @SerialName("end_time") val endTime: String = "23:59",
    val action: String = "block",
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null
) {
    val safeDays: List<String> get() = days ?: emptyList()
}

@Serializable
data class Conflict(
    @SerialName("schedule_a_id") val scheduleAID: String,
    @SerialName("schedule_a_name") val scheduleAName: String,
    @SerialName("schedule_b_id") val scheduleBID: String,
    @SerialName("schedule_b_name") val scheduleBName: String,
    val day: String,
    @SerialName("overlap_start") val overlapStart: String,
    @SerialName("overlap_end") val overlapEnd: String,
    @SerialName("action_a") val actionA: String,
    @SerialName("action_b") val actionB: String
)

@Serializable
data class FlowLog(
    val timestamp: String = "",
    val pdid: String = "",
    val action: String = "",
    val bytes: Long = 0
)

@Serializable
data class NetworkStats(
    @SerialName("blocked_events_24h") val blockedEvents24h: Long = 0,
    @SerialName("top_blocked_device_pdid") val topBlockedDevicePDID: String = ""
)

@Serializable
data class User(
    val id: String = "",
    val name: String = ""
)

@Serializable
data class LiasEvent(
    val type: String,
    val timestamp: String,
    @SerialName("device_id") val deviceID: String = "",
    val payload: JsonElement? = null
)

@Serializable
data class DeviceReidentifiedPayload(
    @SerialName("old_pdid") val oldPdid: String = "",
    @SerialName("new_pdid") val newPdid: String = "",
    val reason: String = "",
    @SerialName("migrated_macs") val migratedMacs: List<String>? = emptyList()
) {
    val safeMigratedMacs: List<String> get() = migratedMacs ?: emptyList()
}

@Serializable
data class DeviceEventPayload(
    val pdid: String = "",
    val mac: String = "",
    val ip: String = "",
    val hostname: String = "",
    @SerialName("confirmed_by") val confirmedBy: List<String>? = emptyList()
) {
    val safeConfirmedBy: List<String> get() = confirmedBy ?: emptyList()
}

@Serializable
data class SecurityAlertPayload(
    @SerialName("alert_type") val alertType: String = "",
    val pdid: String = "",
    val details: String = "",
    val timestamp: String = ""
)
