// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/EventRepositoryActions.kt
// Version: 1.0.0
// Purpose: Extension functions for EventRepository to handle optimistic
//          UI updates (instant feedback) with automatic rollback on failure.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.DeviceTagRequest
import com.lias.remote.core.network.Endpoints

suspend fun EventRepository.assignDeviceTag(pdid: String, tagId: String): ApiResult<Unit> {
    val previousTags = _state.value.devices.find { it.pdid == pdid }?.tags

    // Optimistic Update
    _state.value = _state.value.copy(
        devices = _state.value.devices.map { d ->
            if (d.pdid == pdid) d.copy(tags = listOf(tagId)) else d
        }
    )

    val result = api.post<Unit, DeviceTagRequest>(Endpoints.deviceTags(pdid), DeviceTagRequest(tagId))
    
    // Rollback on failure
    if (result !is ApiResult.Success) {
        _state.value = _state.value.copy(
            devices = _state.value.devices.map { d ->
                if (d.pdid == pdid) d.copy(tags = previousTags ?: listOf("generic")) else d
            }
        )
    }
    return result
}

suspend fun EventRepository.savePolicy(policy: Policy): ApiResult<Policy> {
    val existedBefore = _state.value.policies.any { it.id == policy.id }
    
    // Optimistic Update
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

    // Revert to server reality
    if (result is ApiResult.Success) {
        _state.value = _state.value.copy(
            policies = _state.value.policies.map { if (it.id == result.data.id) result.data else it }
        )
    } else {
        refreshAll() // Safest fallback on failure
    }
    return result
}

suspend fun EventRepository.saveSchedule(schedule: Schedule): ApiResult<Schedule> {
    val existedBefore = _state.value.schedules.any { it.id == schedule.id }
    
    // Optimistic Update
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
