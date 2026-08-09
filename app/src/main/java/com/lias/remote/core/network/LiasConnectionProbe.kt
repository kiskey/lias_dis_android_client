// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/LiasConnectionProbe.kt
// Version: 27.6.0
//
// Purpose:
//   Isolated LIAS endpoint verification.
//
// Critical invariant:
//   Probing candidate connection settings MUST NEVER mutate:
//     EventRepository.api.baseUrl
//     EventRepository.api.authToken
//
// Every probe uses a short-lived independent LiasApiClient that shares
// OkHttp connection infrastructure but owns its endpoint configuration.
// ====================================================================

package com.lias.remote.core.network

import java.net.URI
import okhttp3.OkHttpClient

data class ConnectionProbeSuccess(
    val normalizedUrl: String,
    val health: HealthResponse
)

class LiasConnectionProbe(
    private val httpClient:
        OkHttpClient
) {

    suspend fun probe(
        rawUrl: String,
        authToken: String?
    ): ApiResult<ConnectionProbeSuccess> {

        val normalized =
            normalizeAndValidate(
                rawUrl
            )
                ?: return ApiResult
                    .HttpError(
                        code =
                            400,
                        message =
                            "Enter a valid LIAS server address."
                    )

        /*
         * This instance cannot change EventRepository's active client.
         */
        val candidateClient =
            LiasApiClient(
                httpClient
            )
                .apply {

                    baseUrl =
                        normalized

                    this.authToken =
                        authToken
                            ?.trim()
                            ?.ifBlank {
                                null
                            }
                }

        return when (
            val result =
                candidateClient
                    .get<HealthResponse>(
                        Endpoints.HEALTH
                    )
        ) {

            is ApiResult.Success ->

                ApiResult.Success(
                    ConnectionProbeSuccess(
                        normalizedUrl =
                            normalized,
                        health =
                            result.data
                    )
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

    fun normalizeAndValidate(
        rawUrl: String
    ): String? {

        var normalized =
            rawUrl
                .trim()

        if (
            normalized.isBlank()
        ) {
            return null
        }

        val hasHttpScheme =
            normalized.startsWith(
                "http://",
                ignoreCase = true
            )

        val hasHttpsScheme =
            normalized.startsWith(
                "https://",
                ignoreCase = true
            )

        if (
            !hasHttpScheme &&
            !hasHttpsScheme
        ) {
            normalized =
                "http://$normalized"
        }

        normalized =
            normalized.trimEnd(
                '/'
            )

        return try {

            val uri =
                URI(
                    normalized
                )

            val normalizedScheme =
                uri.scheme
                    ?.lowercase()

            val validScheme =
                normalizedScheme ==
                    "http" ||
                    normalizedScheme ==
                    "https"

            val validHost =
                !uri.host
                    .isNullOrBlank()

            val safeAuthority =
                uri.userInfo ==
                    null &&
                    uri.fragment ==
                    null

            if (
                !validScheme ||
                !validHost ||
                !safeAuthority
            ) {
                null
            } else {
                normalized
            }

        } catch (
            _: Exception
        ) {
            null
        }
    }
}
