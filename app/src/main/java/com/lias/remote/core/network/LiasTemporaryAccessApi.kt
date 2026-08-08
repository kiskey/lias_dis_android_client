// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasTemporaryAccessApi.kt
// Version: 10.0.0
//
// Purpose:
//   Strongly typed LIAS temporary-access operations.
//
// Why separate this from generic policy mutations:
//   Pause and Extend are first-class LIAS commands. Their temporary
//   policy lifecycle belongs to the server.
//
//   Android must not recreate:
//     pol_pause_*
//     sched_pause_*
//     pol_extend_device_*
//
//   locally.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.ExtendAccessResponse
import com.lias.remote.core.models.PauseInternetResponse

suspend fun LiasApiClient.pauseDevice(
    pdid: String
): ApiResult<PauseInternetResponse> {

    if (
        pdid.isBlank()
    ) {
        return ApiResult.HttpError(
            code = 400,
            message =
                "A valid device is required."
        )
    }

    return post<
        PauseInternetResponse,
        Unit
    >(
        Endpoints.devicePause(
            pdid
        ),
        Unit
    )
}

suspend fun LiasApiClient.resumeDevice(
    pdid: String
): ApiResult<Unit> {

    if (
        pdid.isBlank()
    ) {
        return ApiResult.HttpError(
            code = 400,
            message =
                "A valid device is required."
        )
    }

    return delete(
        Endpoints.devicePause(
            pdid
        )
    )
}

suspend fun LiasApiClient.extendDevice(
    pdid: String,
    minutes: Int
): ApiResult<ExtendAccessResponse> {

    if (
        pdid.isBlank()
    ) {
        return ApiResult.HttpError(
            code = 400,
            message =
                "A valid device is required."
        )
    }

    if (
        minutes !in
        MIN_EXTENSION_MINUTES..
            MAX_EXTENSION_MINUTES
    ) {
        return ApiResult.HttpError(
            code = 400,
            message =
                "Access extension must be between 1 and 120 minutes."
        )
    }

    return post<
        ExtendAccessResponse,
        ExtendAccessRequest
    >(
        Endpoints.deviceExtend(
            pdid
        ),
        ExtendAccessRequest(
            minutes =
                minutes
        )
    )
}

suspend fun LiasApiClient.cancelDeviceExtend(
    pdid: String
): ApiResult<Unit> {

    if (
        pdid.isBlank()
    ) {
        return ApiResult.HttpError(
            code = 400,
            message =
                "A valid device is required."
        )
    }

    return delete(
        Endpoints.deviceExtend(
            pdid
        )
    )
}

suspend fun LiasApiClient.extendTag(
    tagId: String,
    minutes: Int
): ApiResult<ExtendAccessResponse> {

    if (
        tagId.isBlank()
    ) {
        return ApiResult.HttpError(
            code = 400,
            message =
                "A valid tag is required."
        )
    }

    if (
        minutes !in
        MIN_EXTENSION_MINUTES..
            MAX_EXTENSION_MINUTES
    ) {
        return ApiResult.HttpError(
            code = 400,
            message =
                "Access extension must be between 1 and 120 minutes."
        )
    }

    return post<
        ExtendAccessResponse,
        ExtendAccessRequest
    >(
        Endpoints.tagExtend(
            tagId
        ),
        ExtendAccessRequest(
            minutes =
                minutes
        )
    )
}

suspend fun LiasApiClient.cancelTagExtend(
    tagId: String
): ApiResult<Unit> {

    if (
        tagId.isBlank()
    ) {
        return ApiResult.HttpError(
            code = 400,
            message =
                "A valid tag is required."
        )
    }

    return delete(
        Endpoints.tagExtend(
            tagId
        )
    )
}

const val FIXED_PAUSE_MINUTES =
    60

const val MIN_EXTENSION_MINUTES =
    1

const val MAX_EXTENSION_MINUTES =
    120
