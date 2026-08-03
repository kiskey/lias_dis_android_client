// ====================================================================
// File: app/src/main/java/com/lias/remote/core/models/Models.kt
// Version: 1.1.0
// Audit Fixes: 
//   1. Added DeviceReidentifiedPayload model for SSE event parsing (GAP-C05).
// ====================================================================

package com.lias.remote.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Device(
    val pdid: String,
    @SerialName("identity_tier") val identityTier: String = "tentative",
    @SerialName("identity_anchor") val identityAnchor: String = "",
    @SerialName("canonical_hostname") val canonicalHostname: String = "",
    @SerialName("current_mac") val currentMAC: String = "",
    val macs: List<String> = emptyList(),
    @SerialName("current_ip") val currentIP: String = "",
    val ips: List<String> = emptyList(),
    val hostname: String = "",
    @SerialName("friendly_name") val friendlyName: String = "",
    val manufacturer: String = "",
    val vendor: String = "",
    val model: String = "",
    @SerialName("device_type") val deviceType: String = "",
    val services: List<String> = emptyList(),
    val online: Boolean = false,
    @SerialName("first_seen") val firstSeen: String = "",
    @SerialName("last_seen") val lastSeen: String = "",
    val confidence: Double = 0.0,
    val tags: List<String> = emptyList()
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
    val type: String, // "global", "tag", "device"
    @SerialName("target_id") val targetID: String = "",
    val action: String, // "allow", "block", "schedule"
    @SerialName("schedule_ids") val scheduleIDs: List<String> = emptyList(),
    @SerialName("schedule_id") val scheduleID: String? = null, // Deprecated but handled
    val priority: Int = 50,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    fun getScheduleIDs(): List<String> {
        if (scheduleIDs.isNotEmpty()) return scheduleIDs
        if (!scheduleID.isNullOrEmpty()) return listOf(scheduleID)
        return emptyList()
    }
}

@Serializable
data class Schedule(
    val id: String,
    val name: String,
    val mode: String, // "whitelist", "downtime"
    val timezone: String,
    val rules: List<ScheduleRule>
)

@Serializable
data class ScheduleRule(
    val days: List<String>,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val action: String // "allow", "block"
)

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

// SSE Event Wrapper
@Serializable
data class LiasEvent(
    val type: String,
    val timestamp: String,
    @SerialName("device_id") val deviceID: String,
    val payload: JsonElement? = null
)

// GAP-C05 Fix: Added missing payload model for device.reidentified event
@Serializable
data class DeviceReidentifiedPayload(
    @SerialName("old_pdid") val oldPdid: String,
    @SerialName("new_pdid") val newPdid: String,
    val reason: String = "",
    @SerialName("migrated_macs") val migratedMacs: List<String> = emptyList()
)
