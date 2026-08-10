package com.lias.remote.core.network

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Additive LIAS v1 capability and identity contracts. */
@Serializable
data class CapabilitiesResponse(
    @SerialName("api_version")
    val apiVersion: String = "v1",
    @SerialName("schema_version")
    val schemaVersion: Int = 0,
    @SerialName("min_client_api_version")
    val minClientApiVersion: String = "v1",
    @SerialName("public_device_key")
    val publicDeviceKey: String = "pdid",
    @SerialName("response_compatibility")
    val responseCompatibility: String = "additive",
    val features: List<String> = emptyList()
) {
    fun supports(feature: String): Boolean =
        features.any { it == feature }
}

@Serializable
data class UpstreamState(
    val reachable: Boolean = false,
    @SerialName("legacy_mode")
    val legacyMode: Boolean = false,
    @SerialName("last_capability_check")
    val lastCapabilityCheck: String? = null,
    @SerialName("last_successful_sync")
    val lastSuccessfulSync: String? = null,
    @SerialName("last_sse_event")
    val lastSseEvent: String? = null,
    @SerialName("last_error")
    val lastError: String? = null
)

@Serializable
data class LiasCapabilitiesResponse(
    @SerialName("api_version")
    val apiVersion: String = "v1",
    @SerialName("schema_version")
    val schemaVersion: Int = 0,
    @SerialName("min_client_api_version")
    val minClientApiVersion: String = "v1",
    @SerialName("public_device_key")
    val publicDeviceKey: String = "pdid",
    @SerialName("response_compatibility")
    val responseCompatibility: String = "additive",
    val features: List<String> = emptyList(),
    @SerialName("dis_capabilities")
    val disCapabilities: CapabilitiesResponse? = null,
    val upstream: UpstreamState = UpstreamState()
) {
    fun supports(feature: String): Boolean =
        features.any { it == feature }
}

@Serializable
data class SystemStatusResponse(
    val status: String = "unknown",
    @SerialName("api_version")
    val apiVersion: String = "v1",
    @SerialName("schema_version")
    val schemaVersion: Int = 0,
    val upstream: UpstreamState = UpstreamState()
)

@Serializable
data class LiasSnapshotResponse(
    val revision: Long = 0L,
    val devices: List<Device> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val policies: List<Policy> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val users: List<User> = emptyList(),
    @SerialName("device_effective_statuses")
    val deviceEffectiveStatuses: Map<String, EffectiveStatus> = emptyMap(),
    @SerialName("tag_effective_statuses")
    val tagEffectiveStatuses: Map<String, EffectiveStatus> = emptyMap()
)

@Serializable
data class ServerErrorResponse(
    val error: String? = null,
    val details: String? = null,
    val code: String? = null,
    val retryable: Boolean = false,
    val message: String? = null
) {
    fun bestMessage(): String? =
        details?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
            ?: error?.takeIf { it.isNotBlank() }
            ?: code?.takeIf { it.isNotBlank() }
}

@Serializable
data class IdentityFactor(
    val kind: String = "",
    @SerialName("likelihood_ratio")
    val likelihoodRatio: Double = 0.0,
    val matched: Boolean = false
)

@Serializable
data class IdentityCandidateDevice(
    val pdid: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("current_mac")
    val currentMac: String = "",
    val online: Boolean = false,
    @SerialName("last_seen")
    val lastSeen: String = ""
)

@Serializable
data class IdentityCandidateDetail(
    val id: Long = 0L,
    @SerialName("source_pdid")
    val sourcePdid: String = "",
    @SerialName("target_pdid")
    val targetPdid: String = "",
    val probability: Double = 0.0,
    val ambiguous: Boolean = false,
    val status: String = "pending",
    val factors: List<IdentityFactor> = emptyList(),
    val conflicts: List<IdentityFactor> = emptyList(),
    @SerialName("source_device")
    val sourceDevice: IdentityCandidateDevice? = null,
    @SerialName("target_device")
    val targetDevice: IdentityCandidateDevice? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    @SerialName("decision_source")
    val decisionSource: String? = null,
    @SerialName("decision_note")
    val decisionNote: String? = null
) {
    val scorePercent: Int
        get() = (probability.coerceIn(0.0, 1.0) * 100.0).toInt()
}

@Serializable
data class IdentityCandidateListResponse(
    val candidates: List<IdentityCandidateDetail> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null
)

@Serializable
data class IdentityCandidateDecisionRequest(
    @SerialName("expected_source_pdid")
    val expectedSourcePdid: String,
    @SerialName("expected_target_pdid")
    val expectedTargetPdid: String,
    @SerialName("expected_updated_at")
    val expectedUpdatedAt: String,
    @SerialName("decision_note")
    val decisionNote: String = ""
)

@Serializable
data class IdentityAlias(
    val id: Long = 0L,
    @SerialName("device_id")
    val deviceId: String = "",
    val pdid: String = "",
    val type: String = "",
    @SerialName("value_hash")
    val valueHash: String = "",
    val source: String = "",
    val confidence: Double = 0.0,
    val verified: Boolean = false,
    @SerialName("first_seen")
    val firstSeen: String = "",
    @SerialName("last_seen")
    val lastSeen: String = "",
    @SerialName("revoked_at")
    val revokedAt: String? = null
)

@Serializable
data class IdentityCandidateLink(
    val id: Long = 0L,
    @SerialName("source_pdid")
    val sourcePdid: String = "",
    @SerialName("target_pdid")
    val targetPdid: String = "",
    val probability: Double = 0.0,
    val ambiguous: Boolean = false,
    val status: String = "",
    val factors: List<IdentityFactor> = emptyList(),
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    @SerialName("decision_source")
    val decisionSource: String? = null,
    @SerialName("decision_note")
    val decisionNote: String? = null
)

@Serializable
data class IdentityEvidence(
    val id: Long = 0L,
    @SerialName("device_id")
    val deviceId: String = "",
    @SerialName("candidate_device_id")
    val candidateDeviceId: String? = null,
    val kind: String = "",
    @SerialName("value_hash")
    val valueHash: String? = null,
    val source: String = "",
    @SerialName("log_likelihood")
    val logLikelihood: Double = 0.0,
    @SerialName("observed_at")
    val observedAt: String = "",
    @SerialName("expires_at")
    val expiresAt: String? = null
)

@Serializable
data class IdentityProfile(
    @SerialName("device_id")
    val deviceId: String = "",
    val pdid: String = "",
    val assurance: String = "unverified",
    val probability: Double = 0.0,
    val ambiguous: Boolean = false,
    val aliases: List<IdentityAlias> = emptyList(),
    val candidates: List<IdentityCandidateLink> = emptyList(),
    val evidence: List<IdentityEvidence> = emptyList()
)

@Serializable
data class IdentityBindingRequest(
    val type: String,
    val value: String,
    val source: String = "lias_android"
)

@Serializable
data class IdentitySplitRequest(
    val mac: String,
    @SerialName("move_ips")
    val moveIps: List<String> = emptyList()
)

object EngineFeatures {
    const val SNAPSHOT = "snapshot_v1"
    const val IDENTITY_CANDIDATES = "identity_candidates"
    const val IDENTITY_CANDIDATE_QUEUE = "identity_candidate_queue"
    const val IDENTITY_CANDIDATE_REOPEN = "identity_candidate_reopen"
    const val IDENTITY_BINDINGS = "identity_bindings"
    const val IDENTITY_SPLIT = "identity_split"
}

internal sealed interface SnapshotFetchResult {
    data class Modified(
        val snapshot: LiasSnapshotResponse,
        val etag: String?
    ) : SnapshotFetchResult

    data object NotModified : SnapshotFetchResult
}
