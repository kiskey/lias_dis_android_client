// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ApiResult.kt
// Version: 22.0.0
//
// Purpose:
//   Canonical LIAS REST result taxonomy.
//
// Design:
//   Network transport, authentication, HTTP semantics and payload
//   decoding are different failure domains and must remain distinct.
//
// UI consequence:
//   - ordinary users receive simple actionable messages
//   - advanced diagnostics retain technical detail
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.Conflict

sealed class ApiResult<out T> {

    data class Success<T>(
        val data: T
    ) : ApiResult<T>()

    /**
     * Authentication/authorization was rejected by LIAS.
     *
     * Kept separate from HttpError because the recovery action is
     * credential repair, not generic Retry.
     */
    data class AuthenticationError(
        val code: Int,
        val message: String =
            "Authentication failed."
    ) : ApiResult<Nothing>()

    /**
     * A syntactically valid HTTP response whose status represents an
     * application/server failure other than authentication/conflict.
     */
    data class HttpError(
        val code: Int,
        val message: String
    ) : ApiResult<Nothing>()

    /**
     * LIAS policy/schedule semantic conflict.
     */
    data class ConflictError(
        val conflicts: List<Conflict>,
        val message: String =
            "LIAS reported a configuration conflict."
    ) : ApiResult<Nothing>()

    /**
     * Transport-level failure:
     *   DNS
     *   routing
     *   timeout
     *   TLS
     *   refused connection
     *   socket interruption
     */
    data class NetworkError(
        val cause: Throwable
    ) : ApiResult<Nothing>()

    /**
     * HTTP transport succeeded but the returned representation could
     * not be decoded as the contract expected by this client.
     */
    data class SerializationError(
        val message: String,
        val cause: Throwable? = null
    ) : ApiResult<Nothing>()
}

inline fun <T, R> ApiResult<T>.mapSuccess(
    transform: (T) -> R
): ApiResult<R> =
    when (this) {

        is ApiResult.Success ->
            ApiResult.Success(
                transform(
                    data
                )
            )

        is ApiResult.AuthenticationError ->
            this

        is ApiResult.HttpError ->
            this

        is ApiResult.ConflictError ->
            this

        is ApiResult.NetworkError ->
            this

        is ApiResult.SerializationError ->
            this
    }

fun ApiResult<*>.isRetryableTransportFailure():
    Boolean =
    when (this) {

        is ApiResult.NetworkError ->
            true

        is ApiResult.HttpError ->
            code >=
                500

        else ->
            false
    }
