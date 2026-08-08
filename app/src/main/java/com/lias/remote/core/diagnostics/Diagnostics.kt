// ====================================================================
// File: app/src/main/java/com/lias/remote/core/diagnostics/Diagnostics.kt
// Version: 22.0.0
//
// Purpose:
//   Translate low-level failures into:
//     1. normal-user actionable messages
//     2. advanced diagnostic records
//
// Privacy:
//   - Authentication tokens are NEVER stored.
//   - URLs are reduced to scheme://host[:port].
//   - Exception messages are truncated.
// ====================================================================

package com.lias.remote.core.diagnostics

import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.network.ConnectionState
import java.net.URI
import java.time.Instant

enum class DiagnosticKind {
    AUTHENTICATION,
    NETWORK,
    SERVER,
    CONFLICT,
    SERIALIZATION,
    CONNECTION,
    INFORMATION
}

data class DiagnosticRecord(
    val timestamp: String,
    val kind: DiagnosticKind,
    val title: String,
    val summary: String,
    val technicalDetail: String? = null
)

data class UserFacingFailure(
    val title: String,
    val message: String,
    val retryable: Boolean,
    val requiresConnectionSettings: Boolean = false
)

object ErrorPresentation {

    fun from(
        result: ApiResult<*>
    ): UserFacingFailure =
        when (
            result
        ) {

            is ApiResult.Success ->

                UserFacingFailure(
                    title =
                        "Completed",
                    message =
                        "The operation completed successfully.",
                    retryable =
                        false
                )

            is ApiResult.AuthenticationError ->

                UserFacingFailure(
                    title =
                        "Authentication Required",
                    message =
                        if (
                            result.code ==
                            403
                        ) {
                            "LIAS refused this request. Check the configured authentication token and server permissions."
                        } else {
                            "LIAS rejected the configured authentication token."
                        },
                    retryable =
                        false,
                    requiresConnectionSettings =
                        true
                )

            is ApiResult.ConflictError ->

                UserFacingFailure(
                    title =
                        "Configuration Conflict",
                    message =
                        result.message
                            .ifBlank {
                                "This change conflicts with the current LIAS configuration."
                            },
                    retryable =
                        false
                )

            is ApiResult.HttpError ->

                when {

                    result.code ==
                        404 ->

                        UserFacingFailure(
                            title =
                                "Not Found",
                            message =
                                "The requested LIAS resource no longer exists. Refresh the app and try again.",
                            retryable =
                                true
                        )

                    result.code in
                        400..499 ->

                        UserFacingFailure(
                            title =
                                "LIAS Rejected the Change",
                            message =
                                result.message
                                    .ifBlank {
                                        "Review the values and try again."
                                    },
                            retryable =
                                false
                        )

                    else ->

                        UserFacingFailure(
                            title =
                                "LIAS Server Error",
                            message =
                                "LIAS could not complete the request. Try again after the server recovers.",
                            retryable =
                                true
                        )
                }

            is ApiResult.NetworkError ->

                UserFacingFailure(
                    title =
                        "LIAS Unreachable",
                    message =
                        "Check that the LIAS server is running and this device can reach its network.",
                    retryable =
                        true,
                    requiresConnectionSettings =
                        true
                )

            is ApiResult.SerializationError ->

                UserFacingFailure(
                    title =
                        "Incompatible Server Response",
                    message =
                        "LIAS responded, but this app could not understand the returned data. Check server/client compatibility.",
                    retryable =
                        false
                )
        }

    fun diagnostic(
        result: ApiResult<*>,
        endpoint: String? = null
    ): DiagnosticRecord {

        val user =
            from(
                result
            )

        return when (
            result
        ) {

            is ApiResult.Success ->

                record(
                    kind =
                        DiagnosticKind.INFORMATION,
                    title =
                        user.title,
                    summary =
                        user.message,
                    detail =
                        endpoint
                )

            is ApiResult.AuthenticationError ->

                record(
                    kind =
                        DiagnosticKind.AUTHENTICATION,
                    title =
                        user.title,
                    summary =
                        user.message,
                    detail =
                        buildString {

                            append(
                                "HTTP "
                            )

                            append(
                                result.code
                            )

                            endpoint
                                ?.let {
                                    append(
                                        " · "
                                    )

                                    append(
                                        safeEndpoint(
                                            it
                                        )
                                    )
                                }
                        }
                )

            is ApiResult.NetworkError ->

                record(
                    kind =
                        DiagnosticKind.NETWORK,
                    title =
                        user.title,
                    summary =
                        user.message,
                    detail =
                        buildString {

                            append(
                                result.cause
                                    .javaClass
                                    .simpleName
                            )

                            result.cause
                                .message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let {

                                    append(
                                        ": "
                                    )

                                    append(
                                        sanitizeText(
                                            it
                                        )
                                    )
                                }

                            endpoint
                                ?.let {

                                    append(
                                        " · "
                                    )

                                    append(
                                        safeEndpoint(
                                            it
                                        )
                                    )
                                }
                        }
                )

            is ApiResult.HttpError ->

                record(
                    kind =
                        DiagnosticKind.SERVER,
                    title =
                        user.title,
                    summary =
                        user.message,
                    detail =
                        buildString {

                            append(
                                "HTTP "
                            )

                            append(
                                result.code
                            )

                            if (
                                result.message
                                    .isNotBlank()
                            ) {

                                append(
                                    " · "
                                )

                                append(
                                    sanitizeText(
                                        result.message
                                    )
                                )
                            }
                        }
                )

            is ApiResult.ConflictError ->

                record(
                    kind =
                        DiagnosticKind.CONFLICT,
                    title =
                        user.title,
                    summary =
                        user.message,
                    detail =
                        "${result.conflicts.size} conflict(s)"
                )

            is ApiResult.SerializationError ->

                record(
                    kind =
                        DiagnosticKind.SERIALIZATION,
                    title =
                        user.title,
                    summary =
                        user.message,
                    detail =
                        buildString {

                            append(
                                sanitizeText(
                                    result.message
                                )
                            )

                            result.cause
                                ?.javaClass
                                ?.simpleName
                                ?.let {

                                    append(
                                        " · "
                                    )

                                    append(
                                        it
                                    )
                                }
                        }
                )
        }
    }

    fun connectionDiagnostic(
        connectionState: ConnectionState
    ): DiagnosticRecord =
        record(
            kind =
                DiagnosticKind.CONNECTION,
            title =
                "Connection State",
            summary =
                when (
                    connectionState
                ) {

                    ConnectionState.CONNECTED ->
                        "Connected to LIAS."

                    ConnectionState.CONNECTING ->
                        "Opening the LIAS event connection."

                    ConnectionState.RECONNECTING ->
                        "The LIAS event connection was interrupted and is reconnecting."

                    ConnectionState.DISCONNECTED ->
                        "No live LIAS event connection is active."
                }
        )

    fun safeEndpoint(
        raw: String
    ): String =
        try {

            val uri =
                URI(
                    raw
                )

            buildString {

                append(
                    uri.scheme
                        ?: "http"
                )

                append(
                    "://"
                )

                append(
                    uri.host
                        ?: "unknown"
                )

                if (
                    uri.port >=
                    0
                ) {

                    append(
                        ":"
                    )

                    append(
                        uri.port
                    )
                }
            }

        } catch (
            _: Exception
        ) {
            "invalid-endpoint"
        }

    private fun record(
        kind: DiagnosticKind,
        title: String,
        summary: String,
        detail: String? = null
    ): DiagnosticRecord =
        DiagnosticRecord(
            timestamp =
                Instant.now()
                    .toString(),
            kind =
                kind,
            title =
                title,
            summary =
                summary,
            technicalDetail =
                detail
                    ?.takeIf {
                        it.isNotBlank()
                    }
        )

    private fun sanitizeText(
        raw: String
    ): String =
        raw
            .replace(
                Regex(
                    "(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+"
                ),
                "Bearer [REDACTED]"
            )
            .replace(
                Regex(
                    "(?i)(token[=: ]+)[^\\s,&]+"
                ),
                "$1[REDACTED]"
            )
            .take(
                512
            )
}
