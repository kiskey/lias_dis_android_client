// ====================================================================
// File: app/src/main/java/com/lias/remote/repositories/TemporaryAccessRepository.kt
// Version: 10.0.0
//
// Purpose:
//   Authoritative temporary-access operations for EventRepository.
//
// Rules:
//   - Mutate through dedicated server endpoints.
//   - Never construct temporary Policy/Schedule objects locally.
//   - Re-fetch EffectiveStatus immediately after successful mutation.
//   - SSE remains the long-lived reconciliation mechanism.
// ====================================================================

package com.lias.remote.repositories

import com.lias.remote.core.models.EffectiveStatus
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
): ApiResult<PauseInternetResponse> {

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
         * Policy and schedule lists also changed server-side.
         *
         * refreshAll() ensures:
         *   - pol_pause_<pdid> appears in policy state
         *   - server-created sched_pause_* appears in schedules
         *   - stale temporary objects disappear after resume/expiry
         */
        refreshAll()
    }

    return result
}

suspend fun EventRepository.resumeDeviceAuthoritatively(
    pdid: String
): ApiResult<Unit> {

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

    return result
}

suspend fun EventRepository.extendDeviceAuthoritatively(
    pdid: String,
    minutes: Int
): ApiResult<ExtendAccessResponse> {

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

    return result
}

suspend fun EventRepository.cancelDeviceExtensionAuthoritatively(
    pdid: String
): ApiResult<Unit> {

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

    return result
}

suspend fun EventRepository.extendTagAuthoritatively(
    tagId: String,
    minutes: Int
): ApiResult<ExtendAccessResponse> {

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

    return result
}

suspend fun EventRepository.cancelTagExtensionAuthoritatively(
    tagId: String
): ApiResult<Unit> {

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

    return result
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

            _state.update { current ->

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

            _state.update { current ->

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
