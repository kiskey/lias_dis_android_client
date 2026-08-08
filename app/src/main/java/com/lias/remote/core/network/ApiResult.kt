// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ApiResult.kt
// Version: 2.0.0
//
// Purpose:
//   Canonical result type for all LIAS Remote REST operations.
//
// Audit / Stability Changes:
//   1. Preserves the existing public result categories.
//   2. Adds explicit SerializationError so malformed successful
//      responses are not incorrectly classified as transport failures.
//   3. Adds AuthenticationError for HTTP 401/403 handling.
//   4. Adds a stable message to ConflictError while preserving the
//      existing conflict list.
//   5. Keeps covariance so existing repository code remains compatible.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.Conflict

sealed class ApiResult<out T> {

    data class Success<T>(
        val data: T
    ) : ApiResult<T>()

    data class HttpError(
        val code: Int,
        val message: String
    ) : ApiResult<Nothing>()

    data class AuthenticationError(
        val code: Int,
        val message: String
    ) : ApiResult<Nothing>()

    data class ConflictError(
        val conflicts: List<Conflict>,
        val message: String = "The requested operation conflicts with existing configuration."
    ) : ApiResult<Nothing>()

    data class NetworkError(
        val cause: Throwable
    ) : ApiResult<Nothing>()

    data class SerializationError(
        val cause: Throwable,
        val body: String? = null
    ) : ApiResult<Nothing>()
}
