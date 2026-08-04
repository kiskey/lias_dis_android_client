// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasApiClient.kt
// Version: 1.3.0
// Audit Fixes:
//   1. Annotated baseUrl and authToken as @Volatile for thread-safe cross-coroutine reads/writes.
//   2. Added automatic URL scheme normalization (http:// prefixing) to prevent
//      OkHttp IllegalArgumentException crashes on bare IP inputs.
//   3. Refactored parseResponse to safely handle empty/204 responses without ClassCastException.
// ====================================================================

package com.lias.remote.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
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

    @Volatile
    var baseUrl: String = "http://127.0.0.1:8081"

    @Volatile
    var authToken: String? = null

    private fun normalizeUrl(raw: String): String {
        var url = raw.trim()
        if (url.isBlank()) return ""
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        return url.trimEnd('/')
    }

    @PublishedApi
    internal fun buildRequest(path: String, method: String, body: RequestBody? = null): Request {
        val sanitizedBase = normalizeUrl(baseUrl)
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
    internal fun <T> parseResponse(response: Response, serializer: KSerializer<T>): ApiResult<T> {
        val bodyString = response.body?.string() ?: ""
        return when {
            response.isSuccessful -> {
                if (bodyString.isBlank() || response.code == 204) {
                    if (serializer.descriptor.serialName == "kotlin.Unit") {
                        ApiResult.Success(Unit as T)
                    } else {
                        ApiResult.HttpError(response.code, "Empty payload returned for expected ${serializer.descriptor.serialName}")
                    }
                } else {
                    try {
                        ApiResult.Success(json.decodeFromString(serializer, bodyString))
                    } catch (e: Exception) {
                        ApiResult.NetworkError(e)
                    }
                }
            }
            response.code == 409 -> {
                try {
                    val errorResp = json.decodeFromString(ConflictResponse.serializer(), bodyString)
                    ApiResult.ConflictError(errorResp.conflicts)
                } catch (e: Exception) {
                    ApiResult.HttpError(409, "Conflict parsing failed")
                }
            }
            else -> {
                val errorMsg = try { 
                    json.decodeFromString(ConflictResponse.serializer(), bodyString).message 
                } catch (e: Exception) { bodyString }
                ApiResult.HttpError(response.code, errorMsg ?: "HTTP ${response.code}")
            }
        }
    }

    suspend inline fun <reified T> get(path: String): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(path, "GET")
            val response = client.newCall(request).execute()
            parseResponse(response, serializer())
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend inline fun <reified T, reified B> post(path: String, body: B): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val bodyStr = json.encodeToString(serializer<B>(), body)
            val reqBody = bodyStr.toRequestBody("application/json".toMediaType())
            val request = buildRequest(path, "POST", reqBody)
            val response = client.newCall(request).execute()
            parseResponse(response, serializer())
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend inline fun <reified T, reified B> put(path: String, body: B): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val bodyStr = json.encodeToString(serializer<B>(), body)
            val reqBody = bodyStr.toRequestBody("application/json".toMediaType())
            val request = buildRequest(path, "PUT", reqBody)
            val response = client.newCall(request).execute()
            parseResponse(response, serializer())
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend inline fun <reified T> delete(path: String): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(path, "DELETE")
            val response = client.newCall(request).execute()
            parseResponse(response, serializer())
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
