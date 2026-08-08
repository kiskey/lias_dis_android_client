// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasApiClient.kt
// Version: 2.0.0
//
// Purpose:
//   Resilient REST client for the LIAS Remote Android application.
//
// Audit / Stability Changes:
//   1. Response bodies are always closed through Response.use.
//   2. HTTP 401/403 are represented explicitly as AuthenticationError.
//   3. Successful JSON decoding failures are represented as
//      SerializationError instead of NetworkError.
//   4. Empty successful responses correctly support Unit/204.
//   5. Error payload parsing is tolerant of malformed/non-JSON bodies.
//   6. URL normalization is centralized.
//   7. Request construction remains compatible with existing repository
//      callers and LIAS endpoint contracts.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.EffectiveStatus
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
    @PublishedApi
    internal val client: OkHttpClient
) {

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    @Volatile
    var baseUrl: String = "http://127.0.0.1:8081"

    @Volatile
    var authToken: String? = null

    private fun normalizeUrl(raw: String): String {
        var url = raw.trim()

        if (url.isBlank()) {
            return ""
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        return url.trimEnd('/')
    }

    @PublishedApi
    internal fun buildRequest(
        path: String,
        method: String,
        body: RequestBody? = null
    ): Request {
        val sanitizedBase = normalizeUrl(baseUrl)

        require(sanitizedBase.isNotBlank()) {
            "LIAS server URL is not configured."
        }

        val normalizedPath =
            if (path.startsWith("/")) path else "/$path"

        val builder = Request.Builder()
            .url("$sanitizedBase$normalizedPath")
            .header("Accept", "application/json")

        authToken
            ?.takeIf { it.isNotBlank() }
            ?.let { token ->
                builder.header(
                    "Authorization",
                    "Bearer $token"
                )
            }

        if (body != null) {
            builder.header(
                "Content-Type",
                "application/json"
            )
        }

        return builder
            .method(method, body)
            .build()
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T> parseResponse(
        response: Response,
        serializer: KSerializer<T>
    ): ApiResult<T> {
        return response.use { safeResponse ->

            val bodyString =
                safeResponse.body?.string().orEmpty()

            when {

                safeResponse.isSuccessful -> {
                    if (
                        safeResponse.code == 204 ||
                        bodyString.isBlank()
                    ) {
                        if (
                            serializer.descriptor.serialName ==
                            "kotlin.Unit"
                        ) {
                            ApiResult.Success(Unit as T)
                        } else {
                            ApiResult.HttpError(
                                safeResponse.code,
                                "Empty payload returned for expected " +
                                    serializer.descriptor.serialName
                            )
                        }
                    } else {
                        try {
                            ApiResult.Success(
                                json.decodeFromString(
                                    serializer,
                                    bodyString
                                )
                            )
                        } catch (error: Exception) {
                            ApiResult.SerializationError(
                                cause = error,
                                body = bodyString.take(MAX_ERROR_BODY_LENGTH)
                            )
                        }
                    }
                }

                safeResponse.code == 401 ||
                    safeResponse.code == 403 -> {
                    ApiResult.AuthenticationError(
                        code = safeResponse.code,
                        message = decodeErrorMessage(
                            bodyString,
                            fallback = when (safeResponse.code) {
                                401 -> "Authentication required."
                                else -> "Access denied."
                            }
                        )
                    )
                }

                safeResponse.code == 409 -> {
                    try {
                        val errorResponse =
                            json.decodeFromString(
                                ConflictResponse.serializer(),
                                bodyString
                            )

                        ApiResult.ConflictError(
                            conflicts = errorResponse.conflicts,
                            message = errorResponse.message
                                ?.takeIf { it.isNotBlank() }
                                ?: "The requested operation conflicts with existing configuration."
                        )
                    } catch (_: Exception) {
                        ApiResult.HttpError(
                            code = 409,
                            message = decodeErrorMessage(
                                bodyString,
                                fallback = "Request conflicts with existing configuration."
                            )
                        )
                    }
                }

                else -> {
                    ApiResult.HttpError(
                        code = safeResponse.code,
                        message = decodeErrorMessage(
                            bodyString,
                            fallback = "HTTP ${safeResponse.code}"
                        )
                    )
                }
            }
        }
    }

    private fun decodeErrorMessage(
        body: String,
        fallback: String
    ): String {
        if (body.isBlank()) {
            return fallback
        }

        return try {
            val response =
                json.decodeFromString(
                    ConflictResponse.serializer(),
                    body
                )

            response.message
                ?.takeIf { it.isNotBlank() }
                ?: response.error
                    ?.takeIf { it.isNotBlank() }
                ?: body.take(MAX_ERROR_BODY_LENGTH)
        } catch (_: Exception) {
            body.take(MAX_ERROR_BODY_LENGTH)
        }
    }

    suspend inline fun <reified T> get(
        path: String
    ): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val request =
                    buildRequest(
                        path = path,
                        method = "GET"
                    )

                client
                    .newCall(request)
                    .execute()
                    .let { response ->
                        parseResponse(
                            response,
                            serializer()
                        )
                    }
            } catch (error: Exception) {
                ApiResult.NetworkError(error)
            }
        }

    suspend fun getRaw(
        path: String
    ): ApiResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val request =
                    buildRequest(
                        path = path,
                        method = "GET"
                    )

                client
                    .newCall(request)
                    .execute()
                    .use { response ->

                        val body =
                            response.body?.string().orEmpty()

                        if (response.isSuccessful) {
                            ApiResult.Success(body)
                        } else if (
                            response.code == 401 ||
                            response.code == 403
                        ) {
                            ApiResult.AuthenticationError(
                                code = response.code,
                                message = decodeErrorMessage(
                                    body,
                                    fallback =
                                        if (response.code == 401) {
                                            "Authentication required."
                                        } else {
                                            "Access denied."
                                        }
                                )
                            )
                        } else {
                            ApiResult.HttpError(
                                response.code,
                                decodeErrorMessage(
                                    body,
                                    fallback = "HTTP ${response.code}"
                                )
                            )
                        }
                    }
            } catch (error: Exception) {
                ApiResult.NetworkError(error)
            }
        }

    suspend inline fun <reified T, reified B> post(
        path: String,
        body: B
    ): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val bodyString =
                    json.encodeToString(
                        serializer<B>(),
                        body
                    )

                val requestBody =
                    bodyString.toRequestBody(
                        "application/json".toMediaType()
                    )

                val request =
                    buildRequest(
                        path = path,
                        method = "POST",
                        body = requestBody
                    )

                client
                    .newCall(request)
                    .execute()
                    .let { response ->
                        parseResponse(
                            response,
                            serializer()
                        )
                    }
            } catch (error: Exception) {
                ApiResult.NetworkError(error)
            }
        }

    suspend fun postRawJson(
        path: String,
        jsonPayload: String
    ): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val requestBody =
                    jsonPayload.toRequestBody(
                        "application/json".toMediaType()
                    )

                val request =
                    buildRequest(
                        path = path,
                        method = "POST",
                        body = requestBody
                    )

                client
                    .newCall(request)
                    .execute()
                    .use { response ->

                        if (response.isSuccessful) {
                            ApiResult.Success(Unit)
                        } else if (
                            response.code == 401 ||
                            response.code == 403
                        ) {
                            ApiResult.AuthenticationError(
                                code = response.code,
                                message = decodeErrorMessage(
                                    response.body?.string().orEmpty(),
                                    fallback =
                                        if (response.code == 401) {
                                            "Authentication required."
                                        } else {
                                            "Access denied."
                                        }
                                )
                            )
                        } else {
                            ApiResult.HttpError(
                                response.code,
                                decodeErrorMessage(
                                    response.body?.string().orEmpty(),
                                    fallback = "Import failed."
                                )
                            )
                        }
                    }
            } catch (error: Exception) {
                ApiResult.NetworkError(error)
            }
        }

    suspend inline fun <reified T, reified B> put(
        path: String,
        body: B
    ): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val bodyString =
                    json.encodeToString(
                        serializer<B>(),
                        body
                    )

                val requestBody =
                    bodyString.toRequestBody(
                        "application/json".toMediaType()
                    )

                val request =
                    buildRequest(
                        path = path,
                        method = "PUT",
                        body = requestBody
                    )

                client
                    .newCall(request)
                    .execute()
                    .let { response ->
                        parseResponse(
                            response,
                            serializer()
                        )
                    }
            } catch (error: Exception) {
                ApiResult.NetworkError(error)
            }
        }

    suspend inline fun <reified T> delete(
        path: String
    ): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val request =
                    buildRequest(
                        path = path,
                        method = "DELETE"
                    )

                client
                    .newCall(request)
                    .execute()
                    .let { response ->
                        parseResponse(
                            response,
                            serializer()
                        )
                    }
            } catch (error: Exception) {
                ApiResult.NetworkError(error)
            }
        }

    // ----------------------------------------------------------------
    // Extend Access
    // ----------------------------------------------------------------

    suspend fun extendDeviceAccess(
        pdid: String,
        minutes: Int
    ): ApiResult<Unit> {
        return post<Unit, ExtendAccessRequest>(
            path = Endpoints.deviceExtend(pdid),
            body = ExtendAccessRequest(
                minutes = minutes
            )
        )
    }

    suspend fun cancelDeviceExtension(
        pdid: String
    ): ApiResult<Unit> {
        return delete<Unit>(
            Endpoints.deviceExtend(pdid)
        )
    }

    suspend fun extendTagAccess(
        tagId: String,
        minutes: Int
    ): ApiResult<Unit> {
        return post<Unit, ExtendAccessRequest>(
            path = Endpoints.tagExtend(tagId),
            body = ExtendAccessRequest(
                minutes = minutes
            )
        )
    }

    suspend fun cancelTagExtension(
        tagId: String
    ): ApiResult<Unit> {
        return delete<Unit>(
            Endpoints.tagExtend(tagId)
        )
    }

    // ----------------------------------------------------------------
    // Effective Status
    // ----------------------------------------------------------------

    suspend fun getDeviceEffectiveStatus(
        pdid: String
    ): ApiResult<EffectiveStatus> {
        return get(
            Endpoints.deviceEffectiveStatus(pdid)
        )
    }

    suspend fun getTagEffectiveStatus(
        tagId: String
    ): ApiResult<EffectiveStatus> {
        return get(
            Endpoints.tagEffectiveStatus(tagId)
        )
    }

    private companion object {
        const val MAX_ERROR_BODY_LENGTH = 4_096
    }
}
