// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/Endpoints.kt
// Version: 2.0.0
//
// Purpose:
//   Single authoritative REST/SSE endpoint catalog for LIAS Remote.
//
// Contract:
//   Paths are aligned with the supplied LIAS backend implementation.
//   Do not construct API paths directly inside UI/repository code.
// ====================================================================

package com.lias.remote.core.network

object Endpoints {

    // ----------------------------------------------------------------
    // Devices
    // ----------------------------------------------------------------

    const val DEVICES = "/api/v1/devices"

    fun device(pdid: String): String =
        "$DEVICES/$pdid"

    fun deviceTags(pdid: String): String =
        "${device(pdid)}/tags"

    fun devicePause(pdid: String): String =
        "${device(pdid)}/pause"

    fun deviceRename(pdid: String): String =
        "${device(pdid)}/rename"

    fun deviceUser(pdid: String): String =
        "${device(pdid)}/user"

    fun deviceLogs(pdid: String): String =
        "${device(pdid)}/logs"

    fun deviceExtend(pdid: String): String =
        "${device(pdid)}/extend"

    fun deviceEffectiveStatus(pdid: String): String =
        "${device(pdid)}/effective-status"


    // ----------------------------------------------------------------
    // Tags
    // ----------------------------------------------------------------

    const val TAGS = "/api/v1/tags"

    fun tag(id: String): String =
        "$TAGS/$id"

    fun tagExtend(tagId: String): String =
        "${tag(tagId)}/extend"

    fun tagEffectiveStatus(tagId: String): String =
        "${tag(tagId)}/effective-status"


    // ----------------------------------------------------------------
    // Policies
    // ----------------------------------------------------------------

    const val POLICIES = "/api/v1/policies"

    const val POLICIES_VALIDATE =
        "$POLICIES/validate"

    const val POLICIES_EXPORT =
        "$POLICIES/export"

    const val POLICIES_IMPORT =
        "$POLICIES/import"

    fun policy(id: String): String =
        "$POLICIES/$id"


    // ----------------------------------------------------------------
    // Schedules
    // ----------------------------------------------------------------

    const val SCHEDULES = "/api/v1/schedules"

    fun schedule(id: String): String =
        "$SCHEDULES/$id"


    // ----------------------------------------------------------------
    // Other LIAS services
    // ----------------------------------------------------------------

    const val USERS = "/api/v1/users"

    const val VACATION =
        "/api/v1/vacation"

    const val STATS =
        "/api/v1/stats"

    const val NFTABLES_FLUSH =
        "/api/v1/nftables/flush"


    // ----------------------------------------------------------------
    // Server-Sent Events
    // ----------------------------------------------------------------

    const val EVENTS_SSE =
        "/api/v1/events"


    // ----------------------------------------------------------------
    // Health
    // ----------------------------------------------------------------

    const val HEALTH =
        "/health"
}
