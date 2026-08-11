package com.lias.remote.repositories

import com.lias.remote.core.models.Device
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.EngineFeatures
import com.lias.remote.core.network.IdentityAlias
import com.lias.remote.core.network.IdentityBindingRequest
import com.lias.remote.core.network.IdentityCandidateDecisionRequest
import com.lias.remote.core.network.IdentityCandidateDetail
import com.lias.remote.core.network.IdentityCandidateListResponse
import com.lias.remote.core.network.IdentityProfile
import com.lias.remote.core.network.IdentitySplitRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private fun EventRepository.identityFeatureAvailable(
    feature: String
): Boolean =
    state.value.supportsEngineFeature(feature)

private fun unsupportedIdentityFeature(): ApiResult.HttpError =
    ApiResult.HttpError(
        code = 501,
        message = "This LIAS server does not advertise the requested identity capability."
    )

suspend fun EventRepository.refreshIdentityCandidates(
    status: String = "pending",
    append: Boolean = false
): ApiResult<IdentityCandidateListResponse> {

    if (
        !identityFeatureAvailable(
            EngineFeatures.IDENTITY_CANDIDATE_QUEUE
        )
    ) {
        return unsupportedIdentityFeature()
    }

    val normalizedStatus =
        status
            .trim()
            .lowercase()
            .takeIf {
                it in setOf("pending", "confirmed", "rejected")
            }
            ?: "pending"

    val current =
        _state.value.identityReview

    val queryRevision =
        beginIdentityQuery()

    val cursor =
        if (
            append &&
            current.status == normalizedStatus
        ) {
            current.nextCursor
        } else {
            null
        }

    _state.value =
        _state.value.copy(
            identityReview =
                current.copy(
                    status = normalizedStatus,
                    isLoading = true,
                    errorMessage = null
                )
        )

    val query =
        buildString {
            append("?status=")
            append(encodeQuery(normalizedStatus))
            append("&limit=50")
            cursor
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append("&cursor=")
                    append(encodeQuery(it))
                }
        }

    val result =
        api.get<IdentityCandidateListResponse>(
            Endpoints.IDENTITY_CANDIDATES + query
        )

    when (result) {
        is ApiResult.Success -> {
            if (!identityQueryIsCurrent(queryRevision)) {
                return result
            }

            val existing =
                if (
                    append &&
                    current.status == normalizedStatus
                ) {
                    current.candidates
                } else {
                    emptyList()
                }

            _state.value =
                _state.value.copy(
                    identityReview =
                        _state.value.identityReview.copy(
                            status = normalizedStatus,
                            candidates =
                                (existing + result.data.candidates)
                                    .distinctBy { it.id },
                            pendingCount =
                                if (normalizedStatus == "pending") {
                                    (existing + result.data.candidates)
                                        .distinctBy { it.id }
                                        .size
                                } else {
                                    _state.value.identityReview.pendingCount
                                },
                            pendingHasMore =
                                if (normalizedStatus == "pending") {
                                    !result.data.nextCursor.isNullOrBlank()
                                } else {
                                    _state.value.identityReview.pendingHasMore
                                },
                            nextCursor = result.data.nextCursor,
                            isLoading = false,
                            errorMessage = null
                        )
                )
        }

        else ->
            if (identityQueryIsCurrent(queryRevision)) {
                _state.value =
                    _state.value.copy(
                        identityReview =
                            _state.value.identityReview.copy(
                                isLoading = false,
                                errorMessage = result.identityErrorMessage()
                            )
                        )
            }
    }

    return result
}

suspend fun EventRepository.loadIdentityCandidate(
    candidateId: Long
): ApiResult<IdentityCandidateDetail> {

    if (
        !identityFeatureAvailable(
            EngineFeatures.IDENTITY_CANDIDATE_QUEUE
        )
    ) {
        return unsupportedIdentityFeature()
    }

    _state.value =
        _state.value.copy(
            identityReview =
                _state.value.identityReview.copy(
                    isLoading = true,
                    errorMessage = null
                )
        )

    val result =
        api.get<IdentityCandidateDetail>(
            Endpoints.identityCandidate(candidateId)
        )

    if (result is ApiResult.Success) {
        val candidate =
            result.data

        val profiles =
            if (
                identityFeatureAvailable(
                    EngineFeatures.IDENTITY_CANDIDATES
                )
            ) {
                coroutineScope {
                listOf(
                    candidate.sourcePdid,
                    candidate.targetPdid
                )
                    .filter { it.isNotBlank() }
                    .distinct()
                    .map { pdid ->
                        pdid to
                            async {
                                api.get<IdentityProfile>(
                                    Endpoints.deviceIdentity(pdid)
                                )
                            }
                    }
                    .mapNotNull { (pdid, deferred) ->
                        when (
                            val profile = deferred.await()
                        ) {
                            is ApiResult.Success ->
                                pdid to profile.data

                            else -> null
                        }
                    }
                    .toMap()
                }
            } else {
                emptyMap()
            }

        _state.value =
            _state.value.copy(
                identityReview =
                    _state.value.identityReview.copy(
                        selectedCandidate = candidate,
                        profiles = profiles,
                        isLoading = false,
                        errorMessage = null
                    )
            )
    } else {
        _state.value =
            _state.value.copy(
                identityReview =
                    _state.value.identityReview.copy(
                        isLoading = false,
                        errorMessage = result.identityErrorMessage()
                    )
            )
    }

    return result
}

fun EventRepository.clearSelectedIdentityCandidate() {
    _state.value =
        _state.value.copy(
            identityReview =
                _state.value.identityReview.copy(
                    selectedCandidate = null,
                    profiles = emptyMap()
                )
        )
}

suspend fun EventRepository.confirmIdentityCandidate(
    candidate: IdentityCandidateDetail,
    note: String
): ApiResult<Device> {
    if (
        !identityFeatureAvailable(
            EngineFeatures.IDENTITY_CANDIDATES
        )
    ) {
        return unsupportedIdentityFeature()
    }

    return identityMutation(
        resourceKey = "identity-candidate:${candidate.id}"
    ) {
        val result =
            api.post<Device, IdentityCandidateDecisionRequest>(
                Endpoints.identityCandidateDecision(
                    candidate.id,
                    "confirm"
                ),
                candidate.decisionRequest(note)
            )

        if (result is ApiResult.Success) {
            clearSelectedIdentityCandidate()
            refreshAll()
            refreshIdentityCandidates()
        } else if (result is ApiResult.ConflictError) {
            // Plan 3.2 stale candidate refresh:
            refreshIdentityCandidates(
                _state.value.identityReview.status
            )
            loadIdentityCandidate(
                candidate.id
            )
            return@identityMutation ApiResult.ConflictError(
                conflicts = result.conflicts,
                message =
                    "This identity review changed on LIAS or is currently unsafe to merge. The latest evidence has been reloaded; review it again before deciding."
            )
        }

        result
    }
}

suspend fun EventRepository.rejectIdentityCandidate(
    candidate: IdentityCandidateDetail,
    note: String
): ApiResult<Unit> {
    if (
        !identityFeatureAvailable(
            EngineFeatures.IDENTITY_CANDIDATES
        )
    ) {
        return unsupportedIdentityFeature()
    }

    return identityMutation(
        resourceKey = "identity-candidate:${candidate.id}"
    ) {
        val result =
            api.post<Unit, IdentityCandidateDecisionRequest>(
                Endpoints.identityCandidateDecision(
                    candidate.id,
                    "reject"
                ),
                candidate.decisionRequest(note)
            )

        if (result is ApiResult.Success) {
            clearSelectedIdentityCandidate()
            refreshIdentityCandidates()
        } else if (result is ApiResult.ConflictError) {
            // Plan 3.2 stale candidate refresh:
            refreshIdentityCandidates(
                _state.value.identityReview.status
            )
            loadIdentityCandidate(
                candidate.id
            )
            return@identityMutation ApiResult.ConflictError(
                conflicts = result.conflicts,
                message =
                    "This identity review changed on LIAS. The latest evidence has been reloaded; review it again before deciding."
            )
        }

        result
    }
}

suspend fun EventRepository.reopenIdentityCandidate(
    candidate: IdentityCandidateDetail,
    note: String
): ApiResult<IdentityCandidateDetail> {

    if (
        !identityFeatureAvailable(
            EngineFeatures.IDENTITY_CANDIDATE_REOPEN
        )
    ) {
        return unsupportedIdentityFeature()
    }

    return identityMutation(
        resourceKey = "identity-candidate:${candidate.id}"
    ) {
        val result =
            api.post<IdentityCandidateDetail, IdentityCandidateDecisionRequest>(
                Endpoints.identityCandidateDecision(
                    candidate.id,
                    "reopen"
                ),
                candidate.decisionRequest(note)
            )

        if (result is ApiResult.Success) {
            clearSelectedIdentityCandidate()
            refreshIdentityCandidates("pending")
            refreshIdentityCandidates("rejected")
        } else if (result is ApiResult.ConflictError) {
            // Plan 3.2 stale candidate refresh:
            refreshIdentityCandidates("rejected")
            loadIdentityCandidate(
                candidate.id
            )
            return@identityMutation ApiResult.ConflictError(
                conflicts = result.conflicts,
                message =
                    "This identity review changed on LIAS. The latest evidence has been reloaded; review it again before reopening."
            )
        }

        result
    }
}

suspend fun EventRepository.bindIdentity(
    pdid: String,
    type: String,
    value: String
): ApiResult<IdentityAlias> {

    if (
        !identityFeatureAvailable(
            EngineFeatures.IDENTITY_BINDINGS
        )
    ) {
        return unsupportedIdentityFeature()
    }

    return identityMutation("identity:$pdid") {
        val result =
            api.post<IdentityAlias, IdentityBindingRequest>(
                Endpoints.deviceIdentityBindings(pdid),
                IdentityBindingRequest(
                    type = type,
                    value = value.trim()
                )
            )

        if (result is ApiResult.Success) {
            refreshSelectedProfiles()
        }

        result
    }
}

suspend fun EventRepository.revokeIdentityBinding(
    pdid: String,
    aliasId: Long
): ApiResult<Unit> {

    if (
        !identityFeatureAvailable(
            EngineFeatures.IDENTITY_BINDINGS
        )
    ) {
        return unsupportedIdentityFeature()
    }

    return identityMutation("identity:$pdid") {
        val result =
            api.delete<Unit>(
                Endpoints.deviceIdentityBinding(
                    pdid,
                    aliasId
                )
            )

        if (result is ApiResult.Success) {
            refreshSelectedProfiles()
        }

        result
    }
}

suspend fun EventRepository.splitIdentity(
    pdid: String,
    mac: String,
    moveIps: List<String> = emptyList()
): ApiResult<Device> {

    if (
        !identityFeatureAvailable(
            EngineFeatures.IDENTITY_SPLIT
        )
    ) {
        return unsupportedIdentityFeature()
    }

    return identityMutation("identity:$pdid") {
        val result =
            api.post<Device, IdentitySplitRequest>(
                Endpoints.deviceIdentitySplit(pdid),
                IdentitySplitRequest(
                    mac = mac.trim(),
                    moveIps = moveIps
                )
            )

        if (result is ApiResult.Success) {
            clearSelectedIdentityCandidate()
            refreshAll()
            refreshIdentityCandidates()
        }

        result
    }
}

private suspend fun EventRepository.refreshSelectedProfiles() {
    val candidate =
        _state.value.identityReview.selectedCandidate
            ?: return

    loadIdentityCandidate(candidate.id)
}

private suspend fun <T> EventRepository.identityMutation(
    resourceKey: String,
    block: suspend () -> ApiResult<T>
): ApiResult<T> =
    mutations.mutate(resourceKey, block)

private fun IdentityCandidateDetail.decisionRequest(
    note: String
): IdentityCandidateDecisionRequest =
    IdentityCandidateDecisionRequest(
        expectedSourcePdid = sourcePdid,
        expectedTargetPdid = targetPdid,
        expectedUpdatedAt = updatedAt,
        decisionNote = note.trim().take(1024)
    )

private fun encodeQuery(value: String): String =
    URLEncoder.encode(
        value,
        StandardCharsets.UTF_8.toString()
    )

private fun ApiResult<*>.identityErrorMessage(): String =
    when (this) {
        is ApiResult.AuthenticationError -> message
        is ApiResult.ConflictError -> message
        is ApiResult.HttpError -> message
        is ApiResult.NetworkError -> "LIAS identity service is unreachable."
        is ApiResult.SerializationError -> message
        is ApiResult.Success -> ""
    }
