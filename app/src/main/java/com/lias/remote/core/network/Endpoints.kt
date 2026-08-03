// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/Endpoints.kt
// Version: 1.0.0
// Purpose: Centralized REST API route constants to prevent typos and
//          ease future API version migrations.
// ====================================================================

package com.lias.remote.core.network

object Endpoints {
    const val DEVICES = "/api/v1/devices"
    fun device(pdid: String) = "/api/v1/devices/$pdid"
    fun deviceTags(pdid: String) = "/api/v1/devices/$pdid/tags"
    
    const val TAGS = "/api/v1/tags"
    fun tag(id: String) = "/api/v1/tags/$id"
    
    const val POLICIES = "/api/v1/policies"
    const val POLICIES_VALIDATE = "/api/v1/policies/validate"
    fun policy(id: String) = "/api/v1/policies/$id"
    
    const val SCHEDULES = "/api/v1/schedules"
    fun schedule(id: String) = "/api/v1/schedules/$id"
    
    const val NFTABLES_FLUSH = "/api/v1/nftables/flush"
    const val EVENTS_SSE = "/api/v1/events"
    
    const val HEALTH = "/health"
}
