// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ApiTypes.kt
// Version: 1.3.0
// Audit Fixes:
//   1. Added ExtendAccessRequest DTO for extend endpoints (§2.3).
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Device
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceListResponse(
    val devices: List<Device> = emptyList(),
    val total: Int = 0
)

@Serializable
data class ConflictResponse(
    val error: String? = null,
    val message: String? = null,
    val conflicts: List<Conflict> = emptyList()
)

@Serializable
data class PolicyValidateRequest(
    @SerialName("schedule_ids") val scheduleIds: List<String>
)

@Serializable
data class DeviceTagRequest(
    @SerialName("tag_id") val tagId: String? = null,
    @SerialName("tag_ids") val tagIds: List<String>? = null
)

@Serializable
data class RenameDeviceRequest(
    val name: String
)

@Serializable
data class UserDeviceRequest(
    @SerialName("user_id") val userId: String
)

@Serializable
data class VacationRequest(
    val enabled: Boolean
)

@Serializable
data class VacationResponse(
    @SerialName("vacation_mode") val vacationMode: Boolean = false
)

@Serializable
data class ExtendAccessRequest(
    val minutes: Int
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String
)
