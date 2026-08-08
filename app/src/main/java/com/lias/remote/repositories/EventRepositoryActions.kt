// ====================================================================
// File:
// app/src/main/java/com/lias/remote/repositories/EventRepositoryActions.kt
// Version: 24.0.0
//
// Purpose:
//   Server-authoritative LIAS mutation operations.
//
// Batch 24:
//   - Removes pause-policy ID inspection.
//   - Dedicated pause endpoint is authoritative.
//   - Serializes repository mutations to avoid overlapping optimistic
//     writes/rollbacks.
//   - New Policy/Schedule objects with id="" are NOT optimistically
//     inserted; the canonical server-generated object is inserted only
//     after POST succeeds.
//   - Handles Batch 22 AuthenticationError and SerializationError.
//   - Normalizes multi-tag generic semantics.
//   - Removes emoji from repository status messages.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.FlowLog
import com.lias.remote.core.models.NetworkStats
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.models.User
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConflictResponse
import com.lias.remote.core.network.DeviceTagRequest
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.PolicyValidateRequest
import com.lias.remote.core.network.RenameDeviceRequest
import com.lias.remote.core.network.UserDeviceRequest
import com.lias.remote.core.network.VacationRequest
import com.lias.remote.core.network.VacationResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One mutation at a time.
 *
 * LIAS normally has one EventRepository instance, but keeping the lock
 * here also protects the extension-function mutation surface itself.
 */
private val mutationMutex =
    Mutex()

private suspend fun <
    T
> serializedMutation(
    block:
        suspend () -> ApiResult<T>
): ApiResult<T> =
    mutationMutex
        .withLock {

            block()
        }

// --------------------------------------------------------------------
// Temporary access
// --------------------------------------------------------------------

suspend fun EventRepository.extendDeviceAccess(
    pdid: String,
    minutes: Int
): ApiResult<Unit> =
    serializedMutation {

        if (
            minutes !in
            1..120
        ) {

            return@serializedMutation ApiResult.HttpError(
                code =
                    400,
                message =
                    "Extension duration must be between 1 and 120 minutes."
            )
        }

        val result =
            api.extendDeviceAccess(
                pdid,
                minutes
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()
        }

        result
    }

suspend fun EventRepository.cancelDeviceExtension(
    pdid: String
): ApiResult<Unit> =
    serializedMutation {

        val result =
            api.cancelDeviceExtension(
                pdid
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()
        }

        result
    }

suspend fun EventRepository.extendTagAccess(
    tagId: String,
    minutes: Int
): ApiResult<Unit> =
    serializedMutation {

        if (
            minutes !in
            1..120
        ) {

            return@serializedMutation ApiResult.HttpError(
                code =
                    400,
                message =
                    "Extension duration must be between 1 and 120 minutes."
            )
        }

        val result =
            api.extendTagAccess(
                tagId,
                minutes
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()
        }

        result
    }

suspend fun EventRepository.cancelTagExtension(
    tagId: String
): ApiResult<Unit> =
    serializedMutation {

        val result =
            api.cancelTagExtension(
                tagId
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()
        }

        result
    }

// --------------------------------------------------------------------
// Effective state
// --------------------------------------------------------------------

suspend fun EventRepository.getDeviceEffectiveStatus(
    pdid: String
): ApiResult<EffectiveStatus> =
    api.getDeviceEffectiveStatus(
        pdid
    )

suspend fun EventRepository.getTagEffectiveStatus(
    tagId: String
): ApiResult<EffectiveStatus> =
    api.getTagEffectiveStatus(
        tagId
    )

// --------------------------------------------------------------------
// Device classification
// --------------------------------------------------------------------

suspend fun EventRepository.assignDeviceTags(
    pdid: String,
    tagIds: List<String>
): ApiResult<Unit> =
    serializedMutation {

        val normalizedTags =
            tagIds
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .toMutableList()
                .apply {

                    if (
                        size >
                        1
                    ) {
                        remove(
                            "generic"
                        )
                    }

                    if (
                        isEmpty()
                    ) {
                        add(
                            "generic"
                        )
                    }
                }

        val previousTags =
            _state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?.safeTags
                ?: listOf(
                    "generic"
                )

        /*
         * Classification changes are safe to show optimistically
         * because the object's canonical identity does not change.
         */
        _state.value =
            _state.value.copy(
                devices =
                    _state.value
                        .devices
                        .map {
                            device ->

                            if (
                                device.pdid ==
                                pdid
                            ) {

                                device.copy(
                                    tags =
                                        normalizedTags
                                )

                            } else {
                                device
                            }
                        }
            )

        val result =
            api.post<
                Unit,
                DeviceTagRequest
            >(
                Endpoints.deviceTags(
                    pdid
                ),
                DeviceTagRequest(
                    tagIds =
                        normalizedTags
                )
            )

        if (
            result !is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    devices =
                        _state.value
                            .devices
                            .map {
                                device ->

                                if (
                                    device.pdid ==
                                    pdid
                                ) {

                                    device.copy(
                                        tags =
                                            previousTags
                                    )

                                } else {
                                    device
                                }
                            }
                )
        }

        result
    }

suspend fun EventRepository.assignDeviceTag(
    pdid: String,
    tagId: String
): ApiResult<Unit> =
    assignDeviceTags(
        pdid =
            pdid,
        tagIds =
            listOf(
                tagId
            )
    )

// --------------------------------------------------------------------
// Pause
// --------------------------------------------------------------------

/**
 * LIAS currently owns Pause as a fixed one-hour operation.
 *
 * Do NOT derive pause state from the backend's internal
 * "pol_pause_<pdid>" implementation detail.
 */
suspend fun EventRepository.pauseDeviceInternet(
    pdid: String
): ApiResult<Unit> =
    serializedMutation {

        val existingStatus =
            _state.value
                .deviceEffectiveStatuses[
                    pdid
                ]

        if (
            existingStatus
                ?.activeExtension
                ?.reasonTag ==
            "pause"
        ) {

            return@serializedMutation ApiResult.Success(
                Unit
            )
        }

        /*
         * Pause handler currently has no request payload.
         *
         * postRawJson gives us Unit semantics even though LIAS returns
         * a small informational JSON object with HTTP 202.
         */
        val result =
            api.postRawJson(
                Endpoints.devicePause(
                    pdid
                ),
                "{}"
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()
        }

        result
    }

suspend fun EventRepository.unpauseDeviceInternet(
    pdid: String
): ApiResult<Unit> =
    serializedMutation {

        val result =
            api.delete<Unit>(
                Endpoints.devicePause(
                    pdid
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()
        }

        result
    }

// --------------------------------------------------------------------
// Device metadata
// --------------------------------------------------------------------

suspend fun EventRepository.renameDevice(
    pdid: String,
    name: String
): ApiResult<Unit> =
    serializedMutation {

        val normalizedName =
            name.trim()

        if (
            normalizedName.isBlank()
        ) {

            return@serializedMutation ApiResult.HttpError(
                code =
                    400,
                message =
                    "Device name cannot be empty."
            )
        }

        val previousName =
            _state.value
                .devices
                .find {
                    it.pdid ==
                        pdid
                }
                ?.friendlyName
                .orEmpty()

        _state.value =
            _state.value.copy(
                devices =
                    _state.value
                        .devices
                        .map {
                            device ->

                            if (
                                device.pdid ==
                                pdid
                            ) {

                                device.copy(
                                    friendlyName =
                                        normalizedName
                                )

                            } else {
                                device
                            }
                        }
            )

        val result =
            api.post<
                Unit,
                RenameDeviceRequest
            >(
                Endpoints.deviceRename(
                    pdid
                ),
                RenameDeviceRequest(
                    normalizedName
                )
            )

        if (
            result !is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    devices =
                        _state.value
                            .devices
                            .map {
                                device ->

                                if (
                                    device.pdid ==
                                    pdid
                                ) {

                                    device.copy(
                                        friendlyName =
                                            previousName
                                    )

                                } else {
                                    device
                                }
                            }
                )
        }

        result
    }

suspend fun EventRepository.getDeviceLogs(
    pdid: String
): ApiResult<List<FlowLog>> =
    api.get(
        Endpoints.deviceLogs(
            pdid
        )
    )

// --------------------------------------------------------------------
// Vacation / maintenance
// --------------------------------------------------------------------

suspend fun EventRepository.toggleVacationMode(
    enabled: Boolean
): ApiResult<VacationResponse> =
    serializedMutation {

        val result =
            api.post<
                VacationResponse,
                VacationRequest
            >(
                Endpoints.VACATION,
                VacationRequest(
                    enabled
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            val statusText =
                if (
                    result.data
                        .vacationMode
                ) {
                    "enabled"
                } else {
                    "disabled"
                }

            _uiEvents.emit(
                UiEvent.ShowSnackbar(
                    "Vacation Mode $statusText"
                )
            )

            refreshAll()
        }

        result
    }

suspend fun EventRepository.flushNftables():
    ApiResult<Unit> =
    serializedMutation {

        val result =
            api.post<
                Unit,
                Unit
            >(
                Endpoints.NFTABLES_FLUSH,
                Unit
            )

        if (
            result is
            ApiResult.Success
        ) {

            _uiEvents.emit(
                UiEvent.ShowSnackbar(
                    "LIAS firewall rules reapplied"
                )
            )

            refreshAll()
        }

        result
    }

// --------------------------------------------------------------------
// Import / export / statistics
// --------------------------------------------------------------------

suspend fun EventRepository.exportPolicies():
    ApiResult<String> =
    api.getRaw(
        Endpoints.POLICIES_EXPORT
    )

suspend fun EventRepository.importPolicies(
    jsonPayload: String
): ApiResult<Unit> =
    serializedMutation {

        val result =
            api.postRawJson(
                Endpoints.POLICIES_IMPORT,
                jsonPayload
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshAll()
        }

        result
    }

suspend fun EventRepository.getNetworkStats():
    ApiResult<NetworkStats> {

    val result =
        api.get<NetworkStats>(
            Endpoints.STATS
        )

    if (
        result is
        ApiResult.Success
    ) {

        _state.value =
            _state.value.copy(
                stats =
                    result.data
            )
    }

    return result
}

// --------------------------------------------------------------------
// Users
// --------------------------------------------------------------------

suspend fun EventRepository.createUser(
    user: User
): ApiResult<User> =
    serializedMutation {

        /*
         * User.id may legitimately be blank here.
         * LIAS returns the canonical persisted User.
         */
        val result =
            api.post<
                User,
                User
            >(
                Endpoints.USERS,
                user.copy(
                    name =
                        user.name
                            .trim()
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    users =
                        (
                            _state.value
                                .users
                                .filterNot {
                                    it.id ==
                                        result.data.id
                                } +
                                result.data
                            )
                            .sortedBy {
                                it.name
                                    .lowercase()
                            }
                )
        }

        result
    }

suspend fun EventRepository.assignDeviceUser(
    pdid: String,
    userId: String
): ApiResult<Unit> =
    serializedMutation {

        val result =
            api.post<
                Unit,
                UserDeviceRequest
            >(
                Endpoints.deviceUser(
                    pdid
                ),
                UserDeviceRequest(
                    userId
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    devices =
                        _state.value
                            .devices
                            .map {
                                device ->

                                if (
                                    device.pdid ==
                                    pdid
                                ) {

                                    device.copy(
                                        userID =
                                            userId
                                                .ifBlank {
                                                    null
                                                }
                                    )

                                } else {
                                    device
                                }
                            }
                )
        }

        result
    }

// --------------------------------------------------------------------
// Policy validation
// --------------------------------------------------------------------

suspend fun EventRepository.validatePolicy(
    scheduleIds: List<String>
): ApiResult<List<Conflict>> {

    val result =
        api.post<
            ConflictResponse,
            PolicyValidateRequest
        >(
            Endpoints.POLICIES_VALIDATE,
            PolicyValidateRequest(
                scheduleIds
            )
        )

    return when (
        result
    ) {

        is ApiResult.Success ->

            ApiResult.Success(
                result.data
                    .conflicts
            )

        /*
         * Validation endpoint uses conflict response as valid
         * information, not transport failure.
         */
        is ApiResult.ConflictError ->

            ApiResult.Success(
                result.conflicts
            )

        is ApiResult.AuthenticationError ->
            result

        is ApiResult.HttpError ->
            result

        is ApiResult.NetworkError ->
            result

        is ApiResult.SerializationError ->
            result
    }
}

// --------------------------------------------------------------------
// Tags
// --------------------------------------------------------------------

suspend fun EventRepository.createTag(
    tag: Tag
): ApiResult<Tag> =
    serializedMutation {

        val result =
            api.post<
                Tag,
                Tag
            >(
                Endpoints.TAGS,
                tag
            )

        if (
            result is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    tags =
                        (
                            _state.value
                                .tags
                                .filterNot {
                                    it.id ==
                                        result.data.id
                                } +
                                result.data
                            )
                )
        }

        result
    }

suspend fun EventRepository.updateTag(
    tag: Tag
): ApiResult<Tag> =
    serializedMutation {

        val previous =
            _state.value
                .tags

        _state.value =
            _state.value.copy(
                tags =
                    previous.map {
                        current ->

                        if (
                            current.id ==
                            tag.id
                        ) {
                            tag
                        } else {
                            current
                        }
                    }
            )

        val result =
            api.put<
                Tag,
                Tag
            >(
                Endpoints.tag(
                    tag.id
                ),
                tag
            )

        when (
            result
        ) {

            is ApiResult.Success ->

                _state.value =
                    _state.value.copy(
                        tags =
                            _state.value
                                .tags
                                .map {
                                    current ->

                                    if (
                                        current.id ==
                                        result.data.id
                                    ) {
                                        result.data
                                    } else {
                                        current
                                    }
                                }
                    )

            else ->

                _state.value =
                    _state.value.copy(
                        tags =
                            previous
                    )
        }

        result
    }

suspend fun EventRepository.deleteTag(
    tagId: String
): ApiResult<Unit> =
    serializedMutation {

        val previous =
            _state.value
                .tags

        _state.value =
            _state.value.copy(
                tags =
                    previous.filterNot {
                        it.id ==
                            tagId
                    }
            )

        val result =
            api.delete<Unit>(
                Endpoints.tag(
                    tagId
                )
            )

        if (
            result !is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    tags =
                        previous
                )
        }

        result
    }

// --------------------------------------------------------------------
// Policies
// --------------------------------------------------------------------

suspend fun EventRepository.savePolicy(
    policy: Policy
): ApiResult<Policy> =
    serializedMutation {

        val existing =
            policy.id
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    id ->

                    _state.value
                        .policies
                        .find {
                            it.id ==
                                id
                        }
                }

        /*
         * CREATE
         *
         * Do NOT put Policy(id="") into state.
         *
         * LIAS owns ID generation, so wait for the canonical response.
         */
        if (
            existing ==
            null
        ) {

            val result =
                api.post<
                    Policy,
                    Policy
                >(
                    Endpoints.POLICIES,
                    policy.copy(
                        id =
                            ""
                    )
                )

            if (
                result is
                ApiResult.Success
            ) {

                _state.value =
                    _state.value.copy(
                        policies =
                            (
                                _state.value
                                    .policies
                                    .filterNot {
                                        it.id ==
                                            result.data.id
                                    } +
                                    result.data
                                )
                    )
            }

            return@serializedMutation result
        }

        /*
         * UPDATE
         */
        val previous =
            _state.value
                .policies

        _state.value =
            _state.value.copy(
                policies =
                    previous.map {
                        current ->

                        if (
                            current.id ==
                            policy.id
                        ) {
                            policy
                        } else {
                            current
                        }
                    }
            )

        val result =
            api.put<
                Policy,
                Policy
            >(
                Endpoints.policy(
                    policy.id
                ),
                policy
            )

        when (
            result
        ) {

            is ApiResult.Success ->

                _state.value =
                    _state.value.copy(
                        policies =
                            _state.value
                                .policies
                                .map {
                                    current ->

                                    if (
                                        current.id ==
                                        result.data.id
                                    ) {
                                        result.data
                                    } else {
                                        current
                                    }
                                }
                    )

            else ->

                _state.value =
                    _state.value.copy(
                        policies =
                            previous
                    )
        }

        result
    }

suspend fun EventRepository.deletePolicy(
    policyId: String
): ApiResult<Unit> =
    serializedMutation {

        val previous =
            _state.value
                .policies

        _state.value =
            _state.value.copy(
                policies =
                    previous.filterNot {
                        it.id ==
                            policyId
                    }
            )

        val result =
            api.delete<Unit>(
                Endpoints.policy(
                    policyId
                )
            )

        if (
            result !is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    policies =
                        previous
                )
        }

        result
    }

// --------------------------------------------------------------------
// Schedules
// --------------------------------------------------------------------

suspend fun EventRepository.saveSchedule(
    schedule: Schedule
): ApiResult<Schedule> =
    serializedMutation {

        val existing =
            schedule.id
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    id ->

                    _state.value
                        .schedules
                        .find {
                            it.id ==
                                id
                        }
                }

        /*
         * CREATE
         *
         * Same canonical-ID rule as Policy.
         */
        if (
            existing ==
            null
        ) {

            val result =
                api.post<
                    Schedule,
                    Schedule
                >(
                Endpoints.SCHEDULES,
                schedule.copy(
                    id =
                        ""
                )
            )

            if (
                result is
                ApiResult.Success
            ) {

                _state.value =
                    _state.value.copy(
                        schedules =
                            (
                                _state.value
                                    .schedules
                                    .filterNot {
                                        it.id ==
                                            result.data.id
                                    } +
                                    result.data
                                )
                    )
            }

            return@serializedMutation result
        }

        val previous =
            _state.value
                .schedules

        _state.value =
            _state.value.copy(
                schedules =
                    previous.map {
                        current ->

                        if (
                            current.id ==
                            schedule.id
                        ) {
                            schedule
                        } else {
                            current
                        }
                    }
            )

        val result =
            api.put<
                Schedule,
                Schedule
            >(
                Endpoints.schedule(
                    schedule.id
                ),
                schedule
            )

        when (
            result
        ) {

            is ApiResult.Success ->

                _state.value =
                    _state.value.copy(
                        schedules =
                            _state.value
                                .schedules
                                .map {
                                    current ->

                                    if (
                                        current.id ==
                                        result.data.id
                                    ) {
                                        result.data
                                    } else {
                                        current
                                    }
                                }
                    )

            else ->

                _state.value =
                    _state.value.copy(
                        schedules =
                            previous
                    )
        }

        result
    }

suspend fun EventRepository.deleteSchedule(
    scheduleId: String
): ApiResult<Unit> =
    serializedMutation {

        val previous =
            _state.value
                .schedules

        _state.value =
            _state.value.copy(
                schedules =
                    previous.filterNot {
                        it.id ==
                            scheduleId
                    }
            )

        val result =
            api.delete<Unit>(
                Endpoints.schedule(
                    scheduleId
                )
            )

        if (
            result !is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    schedules =
                        previous
                )
        }

        result
    }
