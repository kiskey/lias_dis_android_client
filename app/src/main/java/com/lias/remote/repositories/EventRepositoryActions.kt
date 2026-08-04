// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepositoryActions.kt
// Version: 1.4.0
// Audit Fixes: 
//   1. Implemented `validatePolicy` to call server-side `/policies/validate` (GAP-A01).
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConflictResponse
import com.lias.remote.core.network.DeviceTagRequest
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.PolicyValidateRequest

suspend fun EventRepository.assignDeviceTag(pdid: String, tagId: String): ApiResult<Unit> {
    val previousTags = _state.value.devices.find { it.pdid == pdid }?.tags

    _state.value = _state.value.copy(
        devices = _state.value.devices.map { d ->
            if (d.pdid == pdid) d.copy(tags = listOf(tagId)) else d
        }
    )

    val result = api.post<Unit, DeviceTagRequest>(Endpoints.deviceTags(pdid), DeviceTagRequest(tagId))
    
    if (result !is ApiResult.Success) {
        _state.value = _state.value.copy(
            devices = _state.value.devices.map { d ->
                if (d.pdid == pdid) d.copy(tags = previousTags ?: listOf("generic")) else d
            }
        )
    }
    return result
}

// GAP-A01 Fix: Server-side validation action
suspend fun EventRepository.validatePolicy(scheduleIds: List<String>): ApiResult<List<Conflict>> {
    return try {
        when (val result = api.post<ConflictResponse, PolicyValidateRequest>(Endpoints.POLICIES_VALIDATE, PolicyValidateRequest(scheduleIds))) {
            is ApiResult.Success -> ApiResult.Success(result.data.conflicts)
            is ApiResult.Conflict -> ApiResult.Success(result.conflicts)
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
        }
    } catch (e: Exception) {
        ApiResult.NetworkError(e)
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
