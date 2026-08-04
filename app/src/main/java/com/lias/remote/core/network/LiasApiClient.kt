// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasApiClient.kt
// Version: 1.2.1
// Audit Fixes: 
//   1. Added @PublishedApi to internal inline functions to fix visibility errors.
// ====================================================================

package com.lias.remote.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class LiasApiClient(
    private val client: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var baseUrl: String = "http://127.0.0.1:8081"
    var authToken: String? = null

    @PublishedApi
    internal fun buildRequest(path: String, method: String, body: RequestBody? = null): Request {
        val sanitizedBase = baseUrl.trimEnd('/')
        val builder = Request.Builder()
            .url("$sanitizedBase$path")
            .header("Accept", "application/json")
        
        authToken?.takeIf { it.isNotBlank() }?.let {
            builder.header("Authorization", "Bearer $it")
        }
        
        if (body != null) {
            builder.header("Content-Type", "application/json")
        }
        
        return builder.method(method, body).build()
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal inline fun <reified T> parseResponse(response: Response): ApiResult<T> {
        val bodyString = response.body?.string() ?: ""
        return when {
            response.isSuccessful -> {
                if (bodyString.isBlank() || response.code == 204) {
                    ApiResult.Success(Unit as T)
                } else {
                    try {
                        ApiResult.Success(json.decodeFromString(bodyString))
                    } catch (e: Exception) {
                        ApiResult.NetworkError(e)
                    }
                }
            }
            response.code == 409 -> {
                try {
                    val errorResp = json.decodeFromString<ConflictResponse>(bodyString)
                    ApiResult.ConflictError(errorResp.conflicts)
                } catch (e: Exception) {
                    ApiResult.HttpError(409, "Conflict parsing failed")
                }
            }
            else -> {
                val errorMsg = try { 
                    json.decodeFromString<ConflictResponse>(bodyString).message 
                } catch (e: Exception) { bodyString }
                ApiResult.HttpError(response.code, errorMsg ?: "HTTP ${response.code}")
            }
        }
    }

    suspend inline fun <reified T> get(path: String): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(path, "GET")
            val response = client.newCall(request).execute()
            parseResponse(response)
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend inline fun <reified T, reified B> post(path: String, body: B): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val bodyStr = json.encodeToString(serializer(), body)
            val reqBody = bodyStr.toRequestBody("application/json".toMediaType())
            val request = buildRequest(path, "POST", reqBody)
            val response = client.newCall(request).execute()
            parseResponse(response)
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend inline fun <reified T, reified B> put(path: String, body: B): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val bodyStr = json.encodeToString(serializer(), body)
            val reqBody = bodyStr.toRequestBody("application/json".toMediaType())
            val request = buildRequest(path, "PUT", reqBody)
            val response = client.newCall(request).execute()
            parseResponse(response)
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend inline fun <reified T> delete(path: String): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(path, "DELETE")
            val response = client.newCall(request).execute()
            parseResponse(response)
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
