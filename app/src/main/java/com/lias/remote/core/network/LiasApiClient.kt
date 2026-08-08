// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasApiClient.kt
// Version: 22.0.0
//
// Purpose:
//   Canonical LIAS REST client.
//
// Batch 22:
//   - 401/403 -> AuthenticationError
//   - malformed successful payload -> SerializationError
//   - 409 preserves server message + conflicts
//   - response bodies are closed deterministically
//   - raw endpoints share the same HTTP classification
//   - request URL is validated before execution
//
// Compatibility:
//   baseUrl/authToken remain mutable because EventRepository currently
//   follows persisted settings. Candidate connection testing no longer
//   mutates this instance; LiasConnectionProbe owns an isolated client.
// ====================================================================

package com.lias.remote.core.network

import com.lias.remote.core.models.EffectiveStatus
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
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

    @PublishedApi
    internal val json =
        Json {
            ignoreUnknownKeys =
                true

            encodeDefaults =
                true
        }

    @Volatile
    var baseUrl: String =
        "http://127.0.0.1:8081"

    @Volatile
    var authToken: String? =
        null

    private fun normalizedBaseUrl():
        String {

        var result =
            baseUrl
                .trim()

        if (
            result.isBlank()
        ) {
            throw IllegalStateException(
                "LIAS server URL is empty."
            )
        }

        if (
            !result.startsWith(
                "http://",
                ignoreCase = true
            ) &&
            !result.startsWith(
                "https://",
                ignoreCase = true
            )
        ) {
            result =
                "http://$result"
        }

        return result
            .trimEnd(
                '/'
            )
    }

    @PublishedApi
    internal fun buildRequest(
        path: String,
        method: String,
        body: RequestBody? = null
    ): Request {

        val base =
            normalizedBaseUrl()

        val normalizedPath =
            if (
                path.startsWith(
                    "/"
                )
            ) {
                path
            } else {
                "/$path"
            }

        val builder =
            Request.Builder()
                .url(
                    "$base$normalizedPath"
                )
                .header(
                    "Accept",
                    "application/json"
                )

        authToken
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                token ->

                builder.header(
                    "Authorization",
                    "Bearer $token"
                )
            }

        if (
            body !=
            null
        ) {

            builder.header(
                "Content-Type",
                "application/json"
            )
        }

        return builder
            .method(
                method,
                body
            )
            .build()
    }

    @Suppress(
        "UNCHECKED_CAST"
    )
    @PublishedApi
    internal fun <T> parseResponse(
        response: Response,
        serializer: KSerializer<T>
    ): ApiResult<T> {

        val code =
            response.code

        val body =
            response.body
                ?.string()
                .orEmpty()

        if (
            code ==
            401 ||
            code ==
            403
        ) {

            return ApiResult
                .AuthenticationError(
                    code =
                        code,
                    message =
                        decodeServerMessage(
                            body
                        )
                            ?: if (
                                code ==
                                401
                            ) {
                                "LIAS rejected the authentication token."
                            } else {
                                "LIAS refused access to this operation."
                            }
                )
        }

        if (
            code ==
            409
        ) {

            val decoded =
                decodeConflictResponse(
                    body
                )

            return ApiResult
                .ConflictError(
                    conflicts =
                        decoded
                            ?.conflicts
                            .orEmpty(),
                    message =
                        decoded
                            ?.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: decoded
                                ?.error
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                            ?: "LIAS reported a configuration conflict."
                )
        }

        if (
            !response.isSuccessful
        ) {

            return ApiResult
                .HttpError(
                    code =
                        code,
                    message =
                        decodeServerMessage(
                            body
                        )
                            ?: body
                                .trim()
                                .take(
                                    MAX_SERVER_ERROR_LENGTH
                                )
                                .takeIf {
                                    it.isNotBlank()
                                }
                            ?: "HTTP $code"
                )
        }

        if (
            body.isBlank() ||
            code ==
            204
        ) {

            return if (
                serializer
                    .descriptor
                    .serialName ==
                "kotlin.Unit"
            ) {

                ApiResult.Success(
                    Unit as T
                )

            } else {

                ApiResult
                    .SerializationError(
                        message =
                            "LIAS returned an empty response where ${serializer.descriptor.serialName} was expected."
                    )
            }
        }

        return try {

            ApiResult.Success(
                json.decodeFromString(
                    serializer,
                    body
                )
            )

        } catch (
            error: SerializationException
        ) {

            ApiResult
                .SerializationError(
                    message =
                        "LIAS returned data that does not match the expected ${serializer.descriptor.serialName} contract.",
                    cause =
                        error
                )

        } catch (
            error: IllegalArgumentException
        ) {

            ApiResult
                .SerializationError(
                    message =
                        "LIAS returned an invalid ${serializer.descriptor.serialName} payload.",
                    cause =
                        error
                )
        }
    }

    suspend inline fun <
        reified T
    > get(
        path: String
    ): ApiResult<T> =
        execute {
            buildRequest(
                path,
                "GET"
            )
        }

    suspend fun getRaw(
        path: String
    ): ApiResult<String> =
        withContext(
            Dispatchers.IO
        ) {

            try {

                val request =
                    buildRequest(
                        path,
                        "GET"
                    )

                client
                    .newCall(
                        request
                    )
                    .execute()
                    .use {
                        response ->

                        parseRawResponse(
                            response
                        )
                    }

            } catch (
                error: Exception
            ) {

                transportFailure(
                    error
                )
            }
        }

    suspend inline fun <
        reified T,
        reified B
    > post(
        path: String,
        body: B
    ): ApiResult<T> {

        val requestBody =
            try {

                json.encodeToString(
                    serializer<B>(),
                    body
                )
                    .toRequestBody(
                        JSON_MEDIA_TYPE
                    )

            } catch (
                error: Exception
            ) {

                return ApiResult
                    .SerializationError(
                        message =
                            "Unable to encode the LIAS request body.",
                        cause =
                            error
                    )
            }

        return execute {
            buildRequest(
                path,
                "POST",
                requestBody
            )
        }
    }

    suspend inline fun <
        reified T,
        reified B
    > put(
        path: String,
        body: B
    ): ApiResult<T> {

        val requestBody =
            try {

                json.encodeToString(
                    serializer<B>(),
                    body
                )
                    .toRequestBody(
                        JSON_MEDIA_TYPE
                    )

            } catch (
                error: Exception
            ) {

                return ApiResult
                    .SerializationError(
                        message =
                            "Unable to encode the LIAS request body.",
                        cause =
                            error
                    )
            }

        return execute {
            buildRequest(
                path,
                "PUT",
                requestBody
            )
        }
    }

    suspend inline fun <
        reified T
    > delete(
        path: String
    ): ApiResult<T> =
        execute {
            buildRequest(
                path,
                "DELETE"
            )
        }

    suspend fun postRawJson(
        path: String,
        jsonPayload: String
    ): ApiResult<Unit> =
        withContext(
            Dispatchers.IO
        ) {

            val body =
                jsonPayload
                    .toRequestBody(
                        JSON_MEDIA_TYPE
                    )

            try {

                client
                    .newCall(
                        buildRequest(
                            path,
                            "POST",
                            body
                        )
                    )
                    .execute()
                    .use {
                        response ->

                        parseUnitResponse(
                            response
                        )
                    }

            } catch (
                error: Exception
            ) {

                transportFailure(
                    error
                )
            }
        }

    suspend fun extendDeviceAccess(
        pdid: String,
        minutes: Int
    ): ApiResult<Unit> =
        post<
            Unit,
            ExtendAccessRequest
        >(
            Endpoints.deviceExtend(
                pdid
            ),
            ExtendAccessRequest(
                minutes
            )
        )

    suspend fun cancelDeviceExtension(
        pdid: String
    ): ApiResult<Unit> =
        delete(
            Endpoints.deviceExtend(
                pdid
            )
        )

    suspend fun extendTagAccess(
        tagId: String,
        minutes: Int
    ): ApiResult<Unit> =
        post<
            Unit,
            ExtendAccessRequest
        >(
            Endpoints.tagExtend(
                tagId
            ),
            ExtendAccessRequest(
                minutes
            )
        )

    suspend fun cancelTagExtension(
        tagId: String
    ): ApiResult<Unit> =
        delete(
            Endpoints.tagExtend(
                tagId
            )
        )

    suspend fun getDeviceEffectiveStatus(
        pdid: String
    ): ApiResult<EffectiveStatus> =
        get(
            Endpoints.deviceEffectiveStatus(
                pdid
            )
        )

    suspend fun getTagEffectiveStatus(
        tagId: String
    ): ApiResult<EffectiveStatus> =
        get(
            Endpoints.tagEffectiveStatus(
                tagId
            )
        )

    @PublishedApi
    internal suspend inline fun <
        reified T
    > execute(
        crossinline request:
            () -> Request
    ): ApiResult<T> =
        withContext(
            Dispatchers.IO
        ) {

            try {

                client
                    .newCall(
                        request()
                    )
                    .execute()
                    .use {
                        response ->

                        parseResponse(
                            response,
                            serializer()
                        )
                    }

            } catch (
                error: Exception
            ) {

                transportFailure(
                    error
                )
            }
        }

    private fun parseRawResponse(
        response: Response
    ): ApiResult<String> {

        val body =
            response.body
                ?.string()
                .orEmpty()

        return classifyRawHttp(
            code =
                response.code,
            successful =
                response.isSuccessful,
            body =
                body
        )
    }

    private fun parseUnitResponse(
        response: Response
    ): ApiResult<Unit> {

        val body =
            response.body
                ?.string()
                .orEmpty()

        val raw =
            classifyRawHttp(
                code =
                    response.code,
                successful =
                    response.isSuccessful,
                body =
                    body
            )

        return when (
            raw
        ) {

            is ApiResult.Success ->
                ApiResult.Success(
                    Unit
                )

            is ApiResult.AuthenticationError ->
                raw

            is ApiResult.HttpError ->
                raw

            is ApiResult.ConflictError ->
                raw

            is ApiResult.NetworkError ->
                raw

            is ApiResult.SerializationError ->
                raw
        }
    }

    private fun classifyRawHttp(
        code: Int,
        successful: Boolean,
        body: String
    ): ApiResult<String> {

        if (
            code ==
            401 ||
            code ==
            403
        ) {

            return ApiResult
                .AuthenticationError(
                    code =
                        code,
                    message =
                        decodeServerMessage(
                            body
                        )
                            ?: "Authentication failed."
                )
        }

        if (
            code ==
            409
        ) {

            val conflict =
                decodeConflictResponse(
                    body
                )

            return ApiResult
                .ConflictError(
                    conflicts =
                        conflict
                            ?.conflicts
                            .orEmpty(),
                    message =
                        conflict
                            ?.message
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "LIAS reported a conflict."
                )
        }

        if (
            !successful
        ) {

            return ApiResult
                .HttpError(
                    code =
                        code,
                    message =
                        decodeServerMessage(
                            body
                        )
                            ?: body
                                .take(
                                    MAX_SERVER_ERROR_LENGTH
                                )
                                .ifBlank {
                                    "HTTP $code"
                                }
                )
        }

        return ApiResult.Success(
            body
        )
    }

    private fun decodeConflictResponse(
        body: String
    ): ConflictResponse? =
        try {

            json.decodeFromString(
                ConflictResponse.serializer(),
                body
            )

        } catch (
            _: Exception
        ) {
            null
        }

    private fun decodeServerMessage(
        body: String
    ): String? {

        val conflict =
            decodeConflictResponse(
                body
            )

        return conflict
            ?.message
            ?.takeIf {
                it.isNotBlank()
            }
            ?: conflict
                ?.error
                ?.takeIf {
                    it.isNotBlank()
                }
    }

    private fun transportFailure(
        error: Exception
    ): ApiResult<Nothing> =
        when (
            error
        ) {

            is SerializationException ->
                ApiResult
                    .SerializationError(
                        message =
                            "Unable to process LIAS data.",
                        cause =
                            error
                    )

            is IOException ->
                ApiResult
                    .NetworkError(
                        error
                    )

            else ->
                ApiResult
                    .NetworkError(
                        error
                    )
        }

    companion object {

        private val JSON_MEDIA_TYPE =
            "application/json"
                .toMediaType()

        private const val MAX_SERVER_ERROR_LENGTH =
            512
    }
}
