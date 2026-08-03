// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ApiResult.kt
// Version: 1.0.0
// Purpose: Sealed class for robust API state handling. Allows the UI
//          to gracefully handle HTTP errors, network drops, and 
//          specific 409 Conflict states without exception nesting.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.Conflict

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    
    // e.g., 400 Bad Request, 404 Not Found, 500 Internal Error
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>()
    
    // 409 Conflict specifically from LIAS schedule validation
    data class Conflict(val conflicts: List<Conflict>) : ApiResult<Nothing>()
    
    // IOException, serialization errors, etc.
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()
}
