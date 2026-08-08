// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/PolicyScheduleMutations.kt
// Version: 15.0.0
//
// Purpose:
//   Canonical policy/schedule persistence with dependency-safe delete.
//
// Batch 15:
//   - Referenced schedules cannot be deleted from Android.
//   - Prevents deliberate LIAS fail-closed BLOCK caused by dangling
//     schedule references.
//   - infrastructure target remains protected.
//   - global_default remains undeletable.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.Conflict
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConflictResponse
import com.lias.remote.core.network.Endpoints
import com.lias.remote.core.network.PolicyValidateRequest
import com.lias.remote.core.util.ConfigurationSafety

suspend fun EventRepository.validatePolicy(
    scheduleIds: List<String>
): ApiResult<List<Conflict>> {

    val normalized =
        scheduleIds
            .filter {
                it.isNotBlank()
            }
            .distinct()

    val result =
        api.post<
            ConflictResponse,
            PolicyValidateRequest
        >(
            Endpoints.POLICIES_VALIDATE,
            PolicyValidateRequest(
                scheduleIds =
                    normalized
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

suspend fun EventRepository.savePolicy(
    policy: Policy
): ApiResult<Policy> {

    if (
        policy.type ==
            "tag" &&
        policy.targetID ==
            ConfigurationSafety
                .INFRASTRUCTURE_TAG_ID
    ) {
        return ApiResult.HttpError(
            code =
                409,
            message =
                "Infrastructure cannot be targeted by access rules."
        )
    }

    if (
        policy.type ==
            "device"
    ) {

        val target =
            _state.value
                .devices
                .find {
                    it.pdid ==
                        policy.targetID
                }

        if (
            target?.safeTags
                ?.contains(
                    ConfigurationSafety
                        .INFRASTRUCTURE_TAG_ID
                ) == true
        ) {
            return ApiResult.HttpError(
                code =
                    409,
                message =
                    "Infrastructure devices are immune to access rules."
            )
        }
    }

    if (
        policy.type ==
            "global" &&
        policy.id.isNotBlank() &&
        policy.id !=
            "global_default"
    ) {
        return ApiResult.HttpError(
            code =
                400,
            message =
                "LIAS supports one global access policy: global_default."
        )
    }

    val mutationKey =
        if (
            policy.id.isBlank()
        ) {
            "policies:create"
        } else {
            "policy:${policy.id}"
        }

    return mutations.mutate(
        resourceKey =
            mutationKey
    ) {

        val result =
            if (
                policy.id.isBlank()
            ) {

                api.post<
                    Policy,
                    Policy
                >(
                    Endpoints.POLICIES,
                    policy
                )

            } else {

                api.put<
                    Policy,
                    Policy
                >(
                    Endpoints.policy(
                        policy.id
                    ),
                    policy
                )
            }

        if (
            result is
            ApiResult.Success
        ) {

            upsertPolicy(
                result.data
            )

            refreshAll()
        }

        result
    }
}

suspend fun EventRepository.deletePolicy(
    policyId: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "policy:$policyId"
    ) {

        if (
            policyId ==
            "global_default"
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    "Global Access cannot be deleted."
            )
        }

        val policy =
            _state.value
                .policies
                .find {
                    it.id ==
                        policyId
                }

        if (
            policy?.targetID ==
                ConfigurationSafety
                    .INFRASTRUCTURE_TAG_ID
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    "Infrastructure policy state is protected."
            )
        }

        val result =
            api.delete<Unit>(
                Endpoints.policy(
                    policyId
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    policies =
                        _state.value
                            .policies
                            .filterNot {
                                it.id ==
                                    policyId
                            }
                )

            refreshAll()
        }

        result
    }

suspend fun EventRepository.saveSchedule(
    schedule: Schedule
): ApiResult<Schedule> {

    val mutationKey =
        if (
            schedule.id.isBlank()
        ) {
            "schedules:create"
        } else {
            "schedule:${schedule.id}"
        }

    return mutations.mutate(
        resourceKey =
            mutationKey
    ) {

        val result =
            if (
                schedule.id.isBlank()
            ) {

                api.post<
                    Schedule,
                    Schedule
                >(
                    Endpoints.SCHEDULES,
                    schedule
                )

            } else {

                api.put<
                    Schedule,
                    Schedule
                >(
                    Endpoints.schedule(
                        schedule.id
                    ),
                    schedule
                )
            }

        if (
            result is
            ApiResult.Success
        ) {

            upsertSchedule(
                result.data
            )

            refreshAll()
        }

        result
    }
}

suspend fun EventRepository.deleteSchedule(
    scheduleId: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "schedule:$scheduleId"
    ) {

        val schedule =
            _state.value
                .schedules
                .find {
                    it.id ==
                        scheduleId
                }
                ?: return@mutate ApiResult.HttpError(
                    code =
                        404,
                    message =
                        "The schedule no longer exists."
                )

        val impact =
            ConfigurationSafety
                .scheduleImpact(
                    schedule =
                        schedule,
                    policies =
                        _state.value
                            .policies
                )

        if (
            impact.hasDependencies
        ) {
            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    buildString {

                        append(
                            "“"
                        )

                        append(
                            schedule.name
                        )

                        append(
                            "” is still used by "
                        )

                        append(
                            impact
                                .referencingPolicies
                                .size
                        )

                        append(
                            if (
                                impact
                                    .referencingPolicies
                                    .size ==
                                1
                            ) {
                                " rule. "
                            } else {
                                " rules. "
                            }
                        )

                        append(
                            "Remove it from those rules before deleting the schedule. Deleting it directly would make those LIAS schedule bundles fail closed to Block."
                        )
                    }
            )
        }

        val result =
            api.delete<Unit>(
                Endpoints.schedule(
                    scheduleId
                )
            )

        if (
            result is
            ApiResult.Success
        ) {

            _state.value =
                _state.value.copy(
                    schedules =
                        _state.value
                            .schedules
                            .filterNot {
                                it.id ==
                                    scheduleId
                            }
                )

            refreshAll()
        }

        result
    }
