// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepositoryActions.kt
// Version: 1.9.0
// Audit Fixes:
//   1. Added state rollback on API errors for optimistic toggles & actions (AUD-07).
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Conflict
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

suspend fun EventRepository.assignDeviceTags(pdid: String, tagIds: List<String>): ApiResult<Unit> {
    val previousTags = _state.value.devices.find { it.pdid == pdid }?.tags

    _state.value = _state.value.copy(
        devices = _state.value.devices.map { d ->
            if (d.pdid == pdid) d.copy(tags = tagIds) else d
        }
    )

    val result = api.post<Unit, DeviceTagRequest>(Endpoints.deviceTags(pdid), DeviceTagRequest(tagIds = tagIds))
    
    if (result !is ApiResult.Success) {
        _state.value = _state.value.copy(
            devices = _state.value.devices.map { d ->
                if (d.pdid == pdid) d.copy(tags = previousTags ?: listOf("generic")) else d
            }
        )
    }
    return result
}

suspend fun EventRepository.assignDeviceTag(pdid: String, tagId: String): ApiResult<Unit> {
    return assignDeviceTags(pdid, listOf(tagId))
}

suspend fun EventRepository.pauseDeviceInternet(pdid: String): ApiResult<Unit> {
    val result = api.post<Unit, Unit>(Endpoints.devicePause(pdid), Unit)
    if (result is ApiResult.Success) {
        refreshAll()
    }
    return result
}

suspend fun EventRepository.unpauseDeviceInternet(pdid: String): ApiResult<Unit> {
    val result = api.delete<Unit>(Endpoints.devicePause(pdid))
    if (result is ApiResult.Success) {
        refreshAll()
    }
    return result
}

suspend fun EventRepository.renameDevice(pdid: String, name: String): ApiResult<Unit> {
    val previousName = _state.value.devices.find { it.pdid == pdid }?.friendlyName

    _state.value = _state.value.copy(
        devices = _state.value.devices.map { d ->
            if (d.pdid == pdid) d.copy(friendlyName = name) else d
        }
    )

    val result = api.post<Unit, RenameDeviceRequest>(Endpoints.deviceRename(pdid), RenameDeviceRequest(name))

    if (result !is ApiResult.Success) {
        _state.value = _state.value.copy(
            devices = _state.value.devices.map { d ->
                if (d.pdid == pdid) d.copy(friendlyName = previousName ?: "") else d
            }
        )
    }
    return result
}

suspend fun EventRepository.getDeviceLogs(pdid: String): ApiResult<List<FlowLog>> {
    return api.get<List<FlowLog>>(Endpoints.deviceLogs(pdid))
}

suspend fun EventRepository.toggleVacationMode(enabled: Boolean): ApiResult<VacationResponse> {
    val result = api.post<VacationResponse, VacationRequest>(Endpoints.VACATION, VacationRequest(enabled))
    if (result is ApiResult.Success) {
        refreshAll()
    }
    return result
}

suspend fun EventRepository.exportPolicies(): ApiResult<String> {
    return api.getRaw(Endpoints.POLICIES_EXPORT)
}

suspend fun EventRepository.importPolicies(jsonPayload: String): ApiResult<Unit> {
    val result = api.postRawJson(Endpoints.POLICIES_IMPORT, jsonPayload)
    if (result is ApiResult.Success) {
        refreshAll()
    }
    return result
}

suspend fun EventRepository.getNetworkStats(): ApiResult<NetworkStats> {
    val result = api.get<NetworkStats>(Endpoints.STATS)
    if (result is ApiResult.Success) {
        _state.value = _state.value.copy(stats = result.data)
    }
    return result
}

suspend fun EventRepository.createUser(user: User): ApiResult<User> {
    val result = api.post<User, User>(Endpoints.USERS, user)
    if (result is ApiResult.Success) {
        _state.value = _state.value.copy(users = _state.value.users + result.data)
    }
    return result
}

suspend fun EventRepository.assignDeviceUser(pdid: String, userId: String): ApiResult<Unit> {
    val result = api.post<Unit, UserDeviceRequest>(Endpoints.deviceUser(pdid), UserDeviceRequest(userId))
    if (result is ApiResult.Success) {
        _state.value = _state.value.copy(
            devices = _state.value.devices.map { d ->
                if (d.pdid == pdid) d.copy(userID = userId) else d
            }
        )
    }
    return result
}

suspend fun EventRepository.validatePolicy(scheduleIds: List<String>): ApiResult<List<Conflict>> {
    val result = api.post<ConflictResponse, PolicyValidateRequest>(Endpoints.POLICIES_VALIDATE, PolicyValidateRequest(scheduleIds))
    return when (result) {
        is ApiResult.Success -> ApiResult.Success(result.data.conflicts)
        is ApiResult.ConflictError -> ApiResult.Success(result.conflicts)
        is ApiResult.HttpError -> result
        is ApiResult.NetworkError -> result
    }
}

suspend fun EventRepository.createTag(tag: Tag): ApiResult<Tag> {
    val result = api.post<Tag, Tag>(Endpoints.TAGS, tag)
    if (result is ApiResult.Success) {
        _state.value = _state.value.copy(tags = _state.value.tags + result.data)
    }
    return result
}

suspend fun EventRepository.updateTag(tag: Tag): ApiResult<Tag> {
    _state.value = _state.value.copy(
        tags = _state.value.tags.map { if (it.id == tag.id) tag else it }
    )
    
    val result = api.put<Tag, Tag>(Endpoints.tag(tag.id), tag)
    
    if (result is ApiResult.Success) {
        _state.value = _state.value.copy(
            tags = _state.value.tags.map { if (it.id == result.data.id) result.data else it }
        )
    } else {
        refreshAll() 
    }
    return result
}

suspend fun EventRepository.deleteTag(tagId: String): ApiResult<Unit> {
    val deletedTag = _state.value.tags.find { it.id == tagId }
    
    _state.value = _state.value.copy(
        tags = _state.value.tags.filterNot { it.id == tagId }
    )

    val result = api.delete<Unit>(Endpoints.tag(tagId))
    
    if (result !is ApiResult.Success && deletedTag != null) {
        _state.value = _state.value.copy(
            tags = _state.value.tags + deletedTag
        )
    }
    return result
}

suspend fun EventRepository.savePolicy(policy: Policy): ApiResult<Policy> {
    val existedBefore = _state.value.policies.any { it.id == policy.id }
    
    val currentPolicies = _state.value.policies.toMutableList()
    val existingIndex = currentPolicies.indexOfFirst { it.id == policy.id }
    if (existingIndex != -1) {
        currentPolicies[existingIndex] = policy
    } else {
        currentPolicies.add(policy)
    }
    _state.value = _state.value.copy(policies = currentPolicies)

    val result = if (existedBefore) {
        api.put<Policy, Policy>(Endpoints.policy(policy.id), policy)
    } else {
        api.post<Policy, Policy>(Endpoints.POLICIES, policy)
    }

    if (result is ApiResult.Success) {
        _state.value = _state.value.copy(
            policies = _state.value.policies.map { if (it.id == result.data.id) result.data else it }
        )
    } else {
        refreshAll() 
    }
    return result
}

suspend fun EventRepository.deletePolicy(policyId: String): ApiResult<Unit> {
    val deletedPolicy = _state.value.policies.find { it.id == policyId }
    
    _state.value = _state.value.copy(
        policies = _state.value.policies.filterNot { it.id == policyId }
    )

    val result = api.delete<Unit>(Endpoints.policy(policyId))
    
    if (result !is ApiResult.Success && deletedPolicy != null) {
        _state.value = _state.value.copy(
            policies = _state.value.policies + deletedPolicy
        )
    }
    return result
}

suspend fun EventRepository.saveSchedule(schedule: Schedule): ApiResult<Schedule> {
    val existedBefore = _state.value.schedules.any { it.id == schedule.id }
    
    val currentSchedules = _state.value.schedules.toMutableList()
    val existingIndex = currentSchedules.indexOfFirst { it.id == schedule.id }
    if (existingIndex != -1) {
        currentSchedules[existingIndex] = schedule
    } else {
        currentSchedules.add(schedule)
    }
    _state.value = _state.value.copy(schedules = currentSchedules)

    val result = if (existedBefore) {
        api.put<Schedule, Schedule>(Endpoints.schedule(schedule.id), schedule)
    } else {
        api.post<Schedule, Schedule>(Endpoints.SCHEDULES, schedule)
    }

    if (result is ApiResult.Success) {
        _state.value = _state.value.copy(
            schedules = _state.value.schedules.map { if (it.id == result.data.id) result.data else it }
        )
    } else {
        refreshAll() 
    }
    return result
}

suspend fun EventRepository.deleteSchedule(scheduleId: String): ApiResult<Unit> {
    val deletedSchedule = _state.value.schedules.find { it.id == scheduleId }
    
    _state.value = _state.value.copy(
        schedules = _state.value.schedules.filterNot { it.id == scheduleId }
    )

    val result = api.delete<Unit>(Endpoints.schedule(scheduleId))
    
    if (result !is ApiResult.Success && deletedSchedule != null) {
        _state.value = _state.value.copy(
            schedules = _state.value.schedules + deletedSchedule
        )
    }
    return result
}
