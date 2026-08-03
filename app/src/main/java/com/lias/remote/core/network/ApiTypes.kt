// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ApiTypes.kt
// Version: 1.1.1
// Audit Fixes: 
//   1. Added missing HealthResponse DTO to match Go server's /health endpoint.
// ====================================================================

package com.lias.remote.core.network

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
    val conflicts: List<com.lias.remote.core.models.Conflict> = emptyList()
)

@Serializable
data class PolicyValidateRequest(
    @SerialName("schedule_ids") val scheduleIds: List<String>
)

@Serializable
data class DeviceTagRequest(
    @SerialName("tag_id") val tagId: String
)

// FIX 1.3 & 3.1: Added HealthResponse DTO
@Serializable
data class HealthResponse(
    val status: String,
    val version: String
)
