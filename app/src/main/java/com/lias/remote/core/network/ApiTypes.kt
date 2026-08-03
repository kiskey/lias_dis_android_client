// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ApiTypes.kt
// Version: 1.0.0
// Purpose: Request/Response DTOs (Data Transfer Objects) matching the
//          Go shared/api types.
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
