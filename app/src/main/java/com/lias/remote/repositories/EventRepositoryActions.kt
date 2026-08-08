// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepositoryActions.kt
// Version: 3.0.0
//
// Purpose:
//   All user-initiated LIAS mutations.
//
// Stability principles:
//   - Mutations are performed through the repository.
//   - Optimistic changes are rolled back on failure.
//   - Successful server responses become authoritative state.
//   - Full refresh is used only when local reconciliation cannot be
//     trusted.
//   - API result variants are handled exhaustively.
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
import kotlinx.coroutines.flow.update

suspend fun EventRepository.extendDeviceAccess(
    pdid: String,
    minutes: Int
): ApiResult<Unit> {
    if (pdid.isBlank() || minutes <= 0) {
        return ApiResult.HttpError(
            code = 400,
            message = "A valid device and positive duration are required."
        )
    }

    val result =
        api.extendDeviceAccess(
            pdid,
            minutes
        )

    if (result is ApiResult.Success) {
        refreshAll()
    }

    return result
}

suspend fun EventRepository.cancelDeviceExtension(
    pdid: String
): ApiResult<Unit> {
    val result =
        api.cancelDeviceExtension(
            pdid
        )

    if (result is ApiResult.Success) {
        refreshAll()
    }

    return result
}

suspend fun EventRepository.extendTagAccess(
    tagId: String,
    minutes: Int
): ApiResult<Unit> {
    if (tagId.isBlank() || minutes <= 0) {
        return ApiResult.HttpError(
            code = 400,
            message = "A valid tag and positive duration are required."
        )
    }

    val result =
        api.extendTagAccess(
            tagId,
            minutes
        )

    if (result is ApiResult.Success) {
        refreshAll()
    }

    return result
}

suspend fun EventRepository.cancelTagExtension(
    tagId: String
): ApiResult<Unit> {
    val result =
        api.cancelTagExtension(
            tagId
        )

    if (result is ApiResult.Success) {
        refreshAll()
    }

    return result
}

suspend fun EventRepository.getDeviceEffectiveStatus(
    pdid: String
): ApiResult<EffectiveStatus> =
    api.getDeviceEffectiveStatus(pdid)

suspend fun EventRepository.getTagEffectiveStatus(
    tagId: String
): ApiResult<EffectiveStatus> =
    api.getTagEffectiveStatus(tagId)

suspend fun EventRepository.assignDeviceTags(
    pdid: String,
    tagIds: List<String>
): ApiResult<Unit> {

    val previousTags =
        state.value.devices
            .find { it.pdid == pdid }
            ?.tags

    state.update { current ->
        current.copy(
            devices = current.devices.map { device ->
                if (device.pdid == pdid) {
                    device.copy(
                        tags = tagIds.distinct()
                    )
                } else {
                    device
                }
            }
        )
    }

    val result =
        api.post<Unit, DeviceTagRequest>(
            Endpoints.deviceTags(pdid),
            DeviceTagRequest(
                tagIds = tagIds.distinct()
            )
        )

    if (result is ApiResult.Success) {
        refreshAll()
    } else {
        state.update { current ->
            current.copy(
                devices = current.devices.map { device ->
                    if (device.pdid == pdid) {
                        device.copy(
                            tags =
                                previousTags
                                    ?: listOf("generic")
                        )
                    } else {
                        device
                    }
                }
            )
        }
    }

    return result
}

suspend fun EventRepository.assignDeviceTag(
    pdid: String,
    tagId: String
): ApiResult<Unit> =
    assignDeviceTags(
        pdid,
        listOf(tagId)
    )

suspend fun EventRepository.pauseDeviceInternet(
    pdid: String
): ApiResult<Unit> {

    if (pdid.isBlank()) {
        return ApiResult.HttpError(
            400,
            "A valid device is required."
        )
    }

    val alreadyPaused =
        state.value.policies.any {
            it.id == "pol_pause_$pdid" &&
                it.enabled
        }

    if (alreadyPaused) {
        return ApiResult.Success(Unit)
    }

    val result =
        api.post<Unit, Unit>(
            Endpoints.devicePause(pdid),
            Unit
        )

    if (result is ApiResult.Success) {
        refreshAll()
    }

    return result
}

suspend fun EventRepository.unpauseDeviceInternet(
    pdid: String
): ApiResult<Unit> {

    val result =
        api.delete<Unit>(
            Endpoints.devicePause(pdid)
        )

    if (result is ApiResult.Success) {
        refreshAll()
    }

    return result
}

suspend fun EventRepository.renameDevice(
    pdid: String,
    name: String
): ApiResult<Unit> {

    val normalizedName =
        name.trim()

    if (pdid.isBlank()) {
        return ApiResult.HttpError(
            400,
            "A valid device is required."
        )
    }

    if (normalizedName.isBlank()) {
        return ApiResult.HttpError(
            400,
            "Device name cannot be empty."
        )
    }

    val previousName =
        state.value.devices
            .find { it.pdid == pdid }
            ?.friendlyName
            .orEmpty()

    state.update { current ->
        current.copy(
            devices = current.devices.map { device ->
                if (device.pdid == pdid) {
                    device.copy(
                        friendlyName = normalizedName
                    )
                } else {
                    device
                }
            }
        )
    }

    val result =
        api.post<Unit, RenameDeviceRequest>(
            Endpoints.deviceRename(pdid),
            RenameDeviceRequest(
                normalizedName
            )
        )

    if (result is ApiResult.Success) {
        refreshSingleDeviceForAction(
            pdid
        )
    } else {
        state.update { current ->
            current.copy(
                devices = current.devices.map { device ->
                    if (device.pdid == pdid) {
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
    }

    return result
}

private suspend fun EventRepository.refreshSingleDeviceForAction(
    pdid: String
) {
    val result =
        api.get<com.lias.remote.core.models.Device>(
            Endpoints.device(pdid)
        )

    if (result is ApiResult.Success) {
        state.update { current ->
            val devices =
                current.devices.toMutableList()

            val index =
                devices.indexOfFirst {
                    it.pdid == pdid
                }

            if (index >= 0) {
                devices[index] =
                    result.data
            }

            current.copy(
                devices = devices
            )
        }
    }
}

suspend fun EventRepository.getDeviceLogs(
    pdid: String
): ApiResult<List<FlowLog>> =
    api.get(
        Endpoints.deviceLogs(pdid)
    )

suspend fun EventRepository.toggleVacationMode(
    enabled: Boolean
): ApiResult<VacationResponse> {

    val result =
        api.post<VacationResponse, VacationRequest>(
            Endpoints.VACATION,
            VacationRequest(enabled)
        )

    if (result is ApiResult.Success) {
        val status =
            if (result.data.vacationMode) {
                "enabled"
            } else {
                "disabled"
            }

        emitUiEvent(
            UiEvent.ShowSnackbar(
                "Vacation Mode $status"
            )
        )

        refreshAll()
    }

    return result
}

suspend fun EventRepository.flushNftables(): ApiResult<Unit> {
    val result =
        api.post<Unit, Unit>(
            Endpoints.NFTABLES_FLUSH,
            Unit
        )

    if (result is ApiResult.Success) {
        emitUiEvent(
            UiEvent.ShowSnackbar(
                "Firewall table flushed."
            )
        )

        refreshAll()
    }

    return result
}

suspend fun EventRepository.exportPolicies(): ApiResult<String> =
    api.getRaw(
        Endpoints.POLICIES_EXPORT
    )

suspend fun EventRepository.importPolicies(
    jsonPayload: String
): ApiResult<Unit> {

    if (jsonPayload.isBlank()) {
        return ApiResult.HttpError(
            400,
            "Policy import payload is empty."
        )
    }

    val result =
        api.postRawJson(
            Endpoints.POLICIES_IMPORT,
            jsonPayload
        )

    if (result is ApiResult.Success) {
        refreshAll()
    }

    return result
}

suspend fun EventRepository.getNetworkStats():
    ApiResult<NetworkStats> {

    val result =
        api.get<NetworkStats>(
            Endpoints.STATS
        )

    if (result is ApiResult.Success) {
        state.update {
            it.copy(
                stats = result.data
            )
        }
    }

    return result
}

suspend fun EventRepository.createUser(
    user: User
): ApiResult<User> {

    val result =
        api.post<User, User>(
            Endpoints.USERS,
            user
        )

    if (result is ApiResult.Success) {
        state.update {
            it.copy(
                users =
                    it.users + result.data
            )
        }
    }

    return result
}

suspend fun EventRepository.assignDeviceUser(
    pdid: String,
    userId: String
): ApiResult<Unit> {

    val result =
        api.post<Unit, UserDeviceRequest>(
            Endpoints.deviceUser(pdid),
            UserDeviceRequest(userId)
        )

    if (result is ApiResult.Success) {
        state.update { current ->
            current.copy(
                devices = current.devices.map { device ->
                    if (device.pdid == pdid) {
                        device.copy(
                            userID = userId
                        )
                    } else {
                        device
                    }
                }
            )
        }
    }

    return result
}

suspend fun EventRepository.validatePolicy(
    scheduleIds: List<String>
): ApiResult<List<Conflict>> {

    val result =
        api.post<ConflictResponse, PolicyValidateRequest>(
            Endpoints.POLICIES_VALIDATE,
            PolicyValidateRequest(
                scheduleIds =
                    scheduleIds.distinct()
            )
        )

    return when (result) {
        is ApiResult.Success ->
            ApiResult.Success(
                result.data.conflicts
            )

        is ApiResult.ConflictError ->
            ApiResult.Success(
                result.conflicts
            )

        is ApiResult.HttpError ->
            result

        is ApiResult.AuthenticationError ->
            result

        is ApiResult.NetworkError ->
            result

        is ApiResult.SerializationError ->
            result
    }
}

suspend fun EventRepository.createTag(
    tag: Tag
): ApiResult<Tag> {

    val result =
        api.post<Tag, Tag>(
            Endpoints.TAGS,
            tag
        )

    if (result is ApiResult.Success) {
        state.update {
            it.copy(
                tags =
                    it.tags + result.data
            )
        }
    }

    return result
}

suspend fun EventRepository.updateTag(
    tag: Tag
): ApiResult<Tag> {

    val previous =
        state.value.tags
            .find { it.id == tag.id }

    state.update { current ->
        current.copy(
            tags =
                current.tags.map {
                    if (it.id == tag.id) {
                        tag
                    } else {
                        it
                    }
                }
        )
    }

    val result =
        api.put<Tag, Tag>(
            Endpoints.tag(tag.id),
            tag
        )

    if (result is ApiResult.Success) {
        state.update { current ->
            current.copy(
                tags =
                    current.tags.map {
                        if (it.id == result.data.id) {
                            result.data
                        } else {
                            it
                        }
                    }
            )
        }
    } else {
        state.update { current ->
            current.copy(
                tags =
                    current.tags.map {
                        if (it.id == tag.id &&
                            previous != null
                        ) {
                            previous
                        } else {
                            it
                        }
                    }
            )
        }
    }

    return result
}

suspend fun EventRepository.deleteTag(
    tagId: String
): ApiResult<Unit> {

    val deleted =
        state.value.tags
            .find { it.id == tagId }

    state.update { current ->
        current.copy(
            tags =
                current.tags.filterNot {
                    it.id == tagId
                }
        )
    }

    val result =
        api.delete<Unit>(
            Endpoints.tag(tagId)
        )

    if (result !is ApiResult.Success &&
        deleted != null
    ) {
        state.update { current ->
            current.copy(
                tags =
                    (current.tags + deleted)
                        .distinctBy { it.id }
            )
        }
    }

    return result
}

suspend fun EventRepository.savePolicy(
    policy: Policy
): ApiResult<Policy> {

    val previous =
        state.value.policies
            .find { it.id == policy.id }

    val existedBefore =
        previous != null

    state.update { current ->
        val updated =
            current.policies
                .filterNot {
                    it.id == policy.id
                }

        current.copy(
            policies =
                updated + policy
        )
    }

    val result =
        if (existedBefore) {
            api.put<Policy, Policy>(
                Endpoints.policy(policy.id),
                policy
            )
        } else {
            api.post<Policy, Policy>(
                Endpoints.POLICIES,
                policy
            )
        }

    if (result is ApiResult.Success) {
        state.update { current ->
            current.copy(
                policies =
                    current.policies.map {
                        if (it.id == result.data.id) {
                            result.data
                        } else {
                            it
                        }
                    }
            )
        }
    } else {
        state.update { current ->
            val restored =
                current.policies
                    .filterNot {
                        it.id == policy.id
                    }

            current.copy(
                policies =
                    if (previous != null) {
                        restored + previous
                    } else {
                        restored
                    }
            )
        }
    }

    return result
}

suspend fun EventRepository.deletePolicy(
    policyId: String
): ApiResult<Unit> {

    val deleted =
        state.value.policies
            .find { it.id == policyId }

    state.update { current ->
        current.copy(
            policies =
                current.policies.filterNot {
                    it.id == policyId
                }
        )
    }

    val result =
        api.delete<Unit>(
            Endpoints.policy(policyId)
        )

    if (result !is ApiResult.Success &&
        deleted != null
    ) {
        state.update { current ->
            current.copy(
                policies =
                    (current.policies + deleted)
                        .distinctBy { it.id }
            )
        }
    }

    return result
}

suspend fun EventRepository.saveSchedule(
    schedule: Schedule
): ApiResult<Schedule> {

    val previous =
        state.value.schedules
            .find { it.id == schedule.id }

    val existedBefore =
        previous != null

    state.update { current ->
        val updated =
            current.schedules
                .filterNot {
                    it.id == schedule.id
                }

        current.copy(
            schedules =
                updated + schedule
        )
    }

    val result =
        if (existedBefore) {
            api.put<Schedule, Schedule>(
                Endpoints.schedule(schedule.id),
                schedule
            )
        } else {
            api.post<Schedule, Schedule>(
                Endpoints.SCHEDULES,
                schedule
            )
        }

    if (result is ApiResult.Success) {
        state.update { current ->
            current.copy(
                schedules =
                    current.schedules.map {
                        if (it.id == result.data.id) {
                            result.data
                        } else {
                            it
                        }
                    }
            )
        }
    } else {
        state.update { current ->
            val restored =
                current.schedules
                    .filterNot {
                        it.id == schedule.id
                    }

            current.copy(
                schedules =
                    if (previous != null) {
                        restored + previous
                    } else {
                        restored
                    }
            )
        }
    }

    return result
}

suspend fun EventRepository.deleteSchedule(
    scheduleId: String
): ApiResult<Unit> {

    val deleted =
        state.value.schedules
            .find { it.id == scheduleId }

    state.update { current ->
        current.copy(
            schedules =
                current.schedules.filterNot {
                    it.id == scheduleId
                }
        )
    }

    val result =
        api.delete<Unit>(
            Endpoints.schedule(scheduleId)
        )

    if (result !is ApiResult.Success &&
        deleted != null
    ) {
        state.update { current ->
            current.copy(
                schedules =
                    (current.schedules + deleted)
                        .distinctBy { it.id }
            )
        }
    }

    return result
}
