// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasApiClient.kt
// Version: 27.0.2
//
// Purpose:
//   Canonical LIAS REST client.
//
// Compiler remediation:
//   Public inline/reified API functions can only reference:
//     - public API
//     - or @PublishedApi internal declarations.
//
//   Therefore:
//     client
//     json
//     buildRequest()
//     parseResponse()
//     execute()
//     transportFailure()
//     JSON_MEDIA_TYPE
//
//   are exposed to the Kotlin inline ABI with @PublishedApi internal.
//
// Architecture retained:
//   - AuthenticationError for 401/403
//   - ConflictError for 409
//   - NetworkError for transport failures
//   - SerializationError for JSON contract failures
//   - server-authoritative EffectiveStatus endpoints
//   - deterministic Response.use { }
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
import okhttp3.MediaType
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
    internal val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Volatile
    var baseUrl: String =
        "http://127.0.0.1:8081"

    @Volatile
    var authToken: String? =
        null

    private fun normalizedBaseUrl(): String {

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
            .trimEnd('/')
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
                path.startsWith("/")
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
            body != null
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

    @Suppress("UNCHECKED_CAST")
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
            code == 401 ||
            code == 403
        ) {

            return ApiResult.AuthenticationError(
                code = code,
                message =
                    decodeServerMessage(
                        body
                    )
                        ?: if (
                            code == 401
                        ) {
                            "LIAS rejected the authentication token."
                        } else {
                            "LIAS refused access to this operation."
                        }
            )
        }

        if (
            code == 409
        ) {

            val conflict =
                decodeConflictResponse(
                    body
                )

            return ApiResult.ConflictError(
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
                        ?: conflict
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

            return ApiResult.HttpError(
                code = code,
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
            code == 204
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

                ApiResult.SerializationError(
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

            ApiResult.SerializationError(
                message =
                    "LIAS returned data that does not match the expected ${serializer.descriptor.serialName} contract.",
                cause =
                    error
            )

        } catch (
            error: IllegalArgumentException
        ) {

            ApiResult.SerializationError(
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
                path = path,
                method = "GET"
            )
        }

    suspend fun getRaw(
        path: String
    ): ApiResult<String> =
        withContext(
            Dispatchers.IO
        ) {

            try {

                client
                    .newCall(
                        buildRequest(
                            path = path,
                            method = "GET"
                        )
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

    internal suspend fun getSnapshot(
        ifNoneMatch: String?
    ): ApiResult<SnapshotFetchResult> =
        withContext(
            Dispatchers.IO
        ) {
            try {
                val request =
                    buildRequest(
                        path = Endpoints.SNAPSHOT,
                        method = "GET"
                    )
                        .newBuilder()
                        .apply {
                            ifNoneMatch
                                ?.takeIf { it.isNotBlank() }
                                ?.let {
                                    header("If-None-Match", it)
                                }
                        }
                        .build()

                client.newCall(request)
                    .execute()
                    .use { response ->
                        if (response.code == 304) {
                            ApiResult.Success(
                                SnapshotFetchResult.NotModified
                            )
                        } else {
                            when (
                                val parsed =
                                    parseResponse(
                                        response,
                                        LiasSnapshotResponse.serializer()
                                    )
                            ) {
                                is ApiResult.Success ->
                                    ApiResult.Success(
                                        SnapshotFetchResult.Modified(
                                            snapshot = parsed.data,
                                            etag = response.header("ETag")
                                        )
                                    )

                                is ApiResult.AuthenticationError -> parsed
                                is ApiResult.ConflictError -> parsed
                                is ApiResult.HttpError -> parsed
                                is ApiResult.NetworkError -> parsed
                                is ApiResult.SerializationError -> parsed
                            }
                        }
                    }
            } catch (error: Exception) {
                transportFailure(error)
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

                return ApiResult.SerializationError(
                    message =
                        "Unable to encode the LIAS request body.",
                    cause =
                        error
                )
            }

        return execute {
            buildRequest(
                path = path,
                method = "POST",
                body = requestBody
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

                return ApiResult.SerializationError(
                    message =
                        "Unable to encode the LIAS request body.",
                    cause =
                        error
                )
            }

        return execute {
            buildRequest(
                path = path,
                method = "PUT",
                body = requestBody
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
                path = path,
                method = "DELETE"
            )
        }

    suspend fun postRawJson(
        path: String,
        jsonPayload: String
    ): ApiResult<Unit> =
        withContext(
            Dispatchers.IO
        ) {

            val requestBody =
                jsonPayload
                    .toRequestBody(
                        JSON_MEDIA_TYPE
                    )

            try {

                client
                    .newCall(
                        buildRequest(
                            path = path,
                            method = "POST",
                            body = requestBody
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
            path =
                Endpoints.deviceExtend(
                    pdid
                ),
            body =
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
            path =
                Endpoints.tagExtend(
                    tagId
                ),
            body =
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

    /**
     * Public inline methods delegate here.
     *
     * Because this function itself is inline and can be reached by
     * public inline callers, every non-public declaration used here
     * must be @PublishedApi internal.
     */
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
                            response =
                                response,
                            serializer =
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

        return when (
            val result =
                classifyRawHttp(
                    code =
                        response.code,
                    successful =
                        response.isSuccessful,
                    body =
                        body
                )
        ) {

            is ApiResult.Success ->

                ApiResult.Success(
                    Unit
                )

            is ApiResult.AuthenticationError ->
                result

            is ApiResult.HttpError ->
                result

            is ApiResult.ConflictError ->
                result

            is ApiResult.NetworkError ->
                result

            is ApiResult.SerializationError ->
                result
        }
    }

    private fun classifyRawHttp(
        code: Int,
        successful: Boolean,
        body: String
    ): ApiResult<String> {

        if (
            code == 401 ||
            code == 403
        ) {

            return ApiResult.AuthenticationError(
                code = code,
                message =
                    decodeServerMessage(
                        body
                    )
                        ?: "Authentication failed."
            )
        }

        if (
            code == 409
        ) {

            val conflict =
                decodeConflictResponse(
                    body
                )

            return ApiResult.ConflictError(
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
                        ?: conflict
                            ?.error
                            ?.takeIf {
                                it.isNotBlank()
                            }
                        ?: "LIAS reported a conflict."
            )
        }

        if (
            !successful
        ) {

            return ApiResult.HttpError(
                code = code,
                message =
                    decodeServerMessage(
                        body
                    )
                        ?: body
                            .trim()
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

        val structured =
            try {
                json.decodeFromString(
                    ServerErrorResponse.serializer(),
                    body
                )
            } catch (_: Exception) {
                null
            }

        structured
            ?.bestMessage()
            ?.let {
                return it
            }

        val decoded =
            decodeConflictResponse(
                body
            )

        return decoded
            ?.message
            ?.takeIf {
                it.isNotBlank()
            }
            ?: decoded
                ?.error
                ?.takeIf {
                    it.isNotBlank()
                }
    }

    /**
     * Must be visible to public inline execute().
     */
    @PublishedApi
    internal fun transportFailure(
        error: Exception
    ): ApiResult<Nothing> =
        when (
            error
        ) {

            is SerializationException ->

                ApiResult.SerializationError(
                    message =
                        "Unable to process LIAS data.",
                    cause =
                        error
                )

            is IOException ->

                ApiResult.NetworkError(
                    error
                )

            else ->

                ApiResult.NetworkError(
                    error
                )
        }

    companion object {

        /**
         * Referenced from public inline post()/put(), therefore this
         * cannot remain private.
         */
        @PublishedApi
        internal val JSON_MEDIA_TYPE:
            MediaType =
            "application/json"
                .toMediaType()

        private const val MAX_SERVER_ERROR_LENGTH =
            512
    }
}
