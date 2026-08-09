// ====================================================================
// File: app/src/main/java/com/lias/remote/core/network/ConnectionValidation.kt
// Version: 4.0.0
//
// Purpose:
//   Centralized validation of user-supplied LIAS server addresses.
//
// Design:
//   - Accepts http:// and https:// explicitly.
//   - Also accepts a bare host/IP and normalizes it to http://.
//   - Rejects malformed URLs before a network request.
//   - Does not attempt network connectivity here.
//   - Keeps validation deterministic and side-effect free.
// ====================================================================

package com.lias.remote.core.network

import java.net.URI

sealed interface ConnectionValidationResult {

    data class Valid(
        val normalizedUrl: String
    ) : ConnectionValidationResult

    data class Invalid(
        val reason: ConnectionValidationError
    ) : ConnectionValidationResult
}

enum class ConnectionValidationError {
    EMPTY,
    INVALID_FORMAT,
    UNSUPPORTED_SCHEME,
    MISSING_HOST,
    INVALID_PORT
}

object ConnectionValidator {

    fun validate(
        rawUrl: String
    ): ConnectionValidationResult {

        val input =
            rawUrl.trim()

        if (input.isBlank()) {
            return ConnectionValidationResult.Invalid(
                ConnectionValidationError.EMPTY
            )
        }

        val candidate =
            if (
                input.startsWith(
                    "http://",
                    ignoreCase = true
                ) ||
                input.startsWith(
                    "https://",
                    ignoreCase = true
                )
            ) {
                input
            } else {
                "http://$input"
            }

        val uri =
            try {
                URI(candidate)
            } catch (_: Exception) {
                return ConnectionValidationResult.Invalid(
                    ConnectionValidationError.INVALID_FORMAT
                )
            }

        val scheme =
            uri.scheme
                ?.lowercase()
                ?: return ConnectionValidationResult.Invalid(
                    ConnectionValidationError.UNSUPPORTED_SCHEME
                )

        if (
            scheme != "http" &&
            scheme != "https"
        ) {
            return ConnectionValidationResult.Invalid(
                ConnectionValidationError.UNSUPPORTED_SCHEME
            )
        }

        val host =
            uri.host

        if (
            host.isNullOrBlank()
        ) {
            return ConnectionValidationResult.Invalid(
                ConnectionValidationError.MISSING_HOST
            )
        }

        if (
            uri.port < -1 ||
            uri.port > 65535
        ) {
            return ConnectionValidationResult.Invalid(
                ConnectionValidationError.INVALID_PORT
            )
        }

        val normalized =
            buildString {

                append(scheme)
                append("://")
                append(host)

                if (uri.port != -1) {
                    append(":")
                    append(uri.port)
                }

                if (
                    !uri.rawPath.isNullOrBlank() &&
                    uri.rawPath != "/"
                ) {
                    append(
                        uri.rawPath.trimEnd('/')
                    )
                }

                if (
                    !uri.rawQuery.isNullOrBlank()
                ) {
                    append("?")
                    append(uri.rawQuery)
                }
            }

        return ConnectionValidationResult.Valid(
            normalizedUrl = normalized
        )
    }
}
