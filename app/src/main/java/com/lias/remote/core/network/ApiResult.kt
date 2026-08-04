// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ApiResult.kt
// Version: 1.1.0
// Audit Fixes: 
//   1. Renamed Conflict to ConflictError to avoid namespace shadowing with models.Conflict.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.Conflict

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>()
    data class ConflictError(val conflicts: List<Conflict>) : ApiResult<Nothing>()
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()
}
