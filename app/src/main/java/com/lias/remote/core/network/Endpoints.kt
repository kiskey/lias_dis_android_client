// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/Endpoints.kt
// Version: 1.2.0
// Audit Fixes:
//   1. Added REST endpoints for Extend Access and Effective Status (§2.2).
// ====================================================================

package com.lias.remote.core.network

object Endpoints {
    const val DEVICES = "/api/v1/devices"
    fun device(pdid: String) = "/api/v1/devices/$pdid"
    fun deviceTags(pdid: String) = "/api/v1/devices/$pdid/tags"
    fun devicePause(pdid: String) = "/api/v1/devices/$pdid/pause"
    fun deviceRename(pdid: String) = "/api/v1/devices/$pdid/rename"
    fun deviceUser(pdid: String) = "/api/v1/devices/$pdid/user"
    fun deviceLogs(pdid: String) = "/api/v1/devices/$pdid/logs"
    fun deviceExtend(pdid: String) = "/api/v1/devices/$pdid/extend"
    fun deviceEffectiveStatus(pdid: String) = "/api/v1/devices/$pdid/effective-status"
    
    const val TAGS = "/api/v1/tags"
    fun tag(id: String) = "/api/v1/tags/$id"
    fun tagExtend(tagId: String) = "/api/v1/tags/$tagId/extend"
    fun tagEffectiveStatus(tagId: String) = "/api/v1/tags/$tagId/effective-status"
    
    const val POLICIES = "/api/v1/policies"
    const val POLICIES_VALIDATE = "/api/v1/policies/validate"
    const val POLICIES_EXPORT = "/api/v1/policies/export"
    const val POLICIES_IMPORT = "/api/v1/policies/import"
    fun policy(id: String) = "/api/v1/policies/$id"
    
    const val SCHEDULES = "/api/v1/schedules"
    fun schedule(id: String) = "/api/v1/schedules/$id"
    
    const val USERS = "/api/v1/users"
    const val VACATION = "/api/v1/vacation"
    const val STATS = "/api/v1/stats"
    const val NFTABLES_FLUSH = "/api/v1/nftables/flush"
    const val EVENTS_SSE = "/api/v1/events"
    
    const val HEALTH = "/health"
}
