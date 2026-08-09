// ====================================================================
// File:
// app/src/main/java/com/lias/remote/repositories/TemporaryAccessRepository.kt
// Version: 27.1.0
//
// Purpose:
//   Canonical authoritative temporary-access operations.
//
// Rules:
//   - dedicated LIAS endpoints only
//   - no locally fabricated temporary Policy/Schedule
//   - per-target mutation serialization
//   - immediate EffectiveStatus refresh
//   - complete authoritative reconciliation
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.ExtendAccessResponse
import com.lias.remote.core.models.PauseInternetResponse
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.cancelDeviceExtend
import com.lias.remote.core.network.cancelTagExtend
import com.lias.remote.core.network.extendDevice
import com.lias.remote.core.network.extendTag
import com.lias.remote.core.network.pauseDevice
import com.lias.remote.core.network.resumeDevice
import kotlinx.coroutines.flow.update

suspend fun EventRepository.pauseDeviceAuthoritatively(
    pdid: String
): ApiResult<PauseInternetResponse> =
    mutations.mutate(
        resourceKey =
            "device:$pdid"
    ) {

        val existingStatus =
            _state.value
                .deviceEffectiveStatuses[
                    pdid
                ]

        if (
            existingStatus
                ?.activeExtension
                ?.reasonTag
                ?.equals(
                    "pause",
                    ignoreCase =
                        true
                ) ==
            true
        ) {

            return@mutate ApiResult.HttpError(
                code =
                    409,
                message =
                    "Internet is already paused for this device."
            )
        }

        val result =
            api.pauseDevice(
                pdid
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshDeviceEffectiveStatus(
                pdid
            )

            /*
             * LIAS may create/remove temporary internal policy and
             * schedule objects.
             *
             * Android refreshes them but NEVER derives pause state from
             * their identifiers.
             */
            refreshAll()
        }

        result
    }

suspend fun EventRepository.resumeDeviceAuthoritatively(
    pdid: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "device:$pdid"
    ) {

        val result =
            api.resumeDevice(
                pdid
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshDeviceEffectiveStatus(
                pdid
            )

            refreshAll()
        }

        result
    }

suspend fun EventRepository.extendDeviceAuthoritatively(
    pdid: String,
    minutes: Int
): ApiResult<ExtendAccessResponse> =
    mutations.mutate(
        resourceKey =
            "device:$pdid"
    ) {

        if (
            minutes !in
            1..120
        ) {

            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "Extension duration must be between 1 and 120 minutes."
            )
        }

        val result =
            api.extendDevice(
                pdid =
                    pdid,
                minutes =
                    minutes
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshDeviceEffectiveStatus(
                pdid
            )

            refreshAll()
        }

        result
    }

suspend fun EventRepository.cancelDeviceExtensionAuthoritatively(
    pdid: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "device:$pdid"
    ) {

        val result =
            api.cancelDeviceExtend(
                pdid
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshDeviceEffectiveStatus(
                pdid
            )

            refreshAll()
        }

        result
    }

suspend fun EventRepository.extendTagAuthoritatively(
    tagId: String,
    minutes: Int
): ApiResult<ExtendAccessResponse> =
    mutations.mutate(
        resourceKey =
            "tag:$tagId"
    ) {

        if (
            minutes !in
            1..120
        ) {

            return@mutate ApiResult.HttpError(
                code =
                    400,
                message =
                    "Extension duration must be between 1 and 120 minutes."
            )
        }

        val result =
            api.extendTag(
                tagId =
                    tagId,
                minutes =
                    minutes
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshTagEffectiveStatus(
                tagId
            )

            refreshAll()
        }

        result
    }

suspend fun EventRepository.cancelTagExtensionAuthoritatively(
    tagId: String
): ApiResult<Unit> =
    mutations.mutate(
        resourceKey =
            "tag:$tagId"
    ) {

        val result =
            api.cancelTagExtend(
                tagId
            )

        if (
            result is
            ApiResult.Success
        ) {

            refreshTagEffectiveStatus(
                tagId
            )

            refreshAll()
        }

        result
    }

private suspend fun EventRepository.refreshDeviceEffectiveStatus(
    pdid: String
) {

    when (
        val result =
            api.getDeviceEffectiveStatus(
                pdid
            )
    ) {

        is ApiResult.Success -> {

            _state.update {
                current ->

                current.copy(
                    deviceEffectiveStatuses =
                        current
                            .deviceEffectiveStatuses +
                            (
                                pdid to
                                    result.data
                                )
                )
            }
        }

        else ->
            Unit
    }
}

private suspend fun EventRepository.refreshTagEffectiveStatus(
    tagId: String
) {

    when (
        val result =
            api.getTagEffectiveStatus(
                tagId
            )
    ) {

        is ApiResult.Success -> {

            _state.update {
                current ->

                current.copy(
                    tagEffectiveStatuses =
                        current
                            .tagEffectiveStatuses +
                            (
                                tagId to
                                    result.data
                                )
                )
            }
        }

        else ->
            Unit
    }
}
