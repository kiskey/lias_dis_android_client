// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/navigation/NavigationRoutes.kt
// Version: 25.0.0
//
// Purpose:
//   Single navigation/deep-link grammar.
//
// Security:
//   Deep links navigate only.
//   They cannot configure a LIAS server or authentication token.
// ====================================================================

package com.lias.remote.ui.navigation

import android.net.Uri

object NavigationRoutes {

    const val HOME =
        "home"

    const val DEVICES =
        "devices"

    const val SCHEDULES =
        "schedules"

    const val RULES =
        "rules"

    const val SETTINGS =
        "settings"

    const val CONNECTION_SETTINGS =
        "connection_settings"

    const val IDENTITY_REVIEW =
        "identity_review"

    const val DEVICE_DETAIL =
        "device_detail/{pdid}"

    fun deviceDetail(
        pdid: String
    ): String =
        "device_detail/${
            Uri.encode(
                pdid
            )
        }"
}

sealed interface ExternalDestination {

    data object Home :
        ExternalDestination

    data object Devices :
        ExternalDestination

    data object Schedules :
        ExternalDestination

    data object Rules :
        ExternalDestination

    data object Settings :
        ExternalDestination

    data class Device(
        val pdid: String
    ) : ExternalDestination
}

object LiasDeepLinks {

    private const val SCHEME =
        "liasremote"

    fun parse(
        raw: String?
    ): ExternalDestination? {

        if (
            raw.isNullOrBlank()
        ) {
            return null
        }

        val uri =
            try {

                Uri.parse(
                    raw
                )

            } catch (
                _: Exception
            ) {
                return null
            }

        if (
            !uri.scheme.equals(
                SCHEME,
                ignoreCase =
                    true
            )
        ) {
            return null
        }

        /*
         * Credentials / endpoint configuration are intentionally not
         * accepted from URI query parameters.
         */
        if (
            uri.getQueryParameter(
                "token"
            ) !=
            null ||
            uri.getQueryParameter(
                "auth_token"
            ) !=
            null ||
            uri.getQueryParameter(
                "server"
            ) !=
            null ||
            uri.getQueryParameter(
                "url"
            ) !=
            null
        ) {
            return null
        }

        val host =
            uri.host
                ?.lowercase()
                ?: return null

        val segments =
            uri.pathSegments
                .filter {
                    it.isNotBlank()
                }

        return when (
            host
        ) {

            "home" ->

                if (
                    segments.isEmpty()
                ) {
                    ExternalDestination.Home
                } else {
                    null
                }

            "devices" ->

                when (
                    segments.size
                ) {

                    0 ->
                        ExternalDestination.Devices

                    1 ->
                        segments
                            .single()
                            .takeIf {
                                isSafeIdentifier(
                                    it
                                )
                            }
                            ?.let {
                                ExternalDestination.Device(
                                    Uri.decode(
                                        it
                                    )
                                )
                            }

                    else ->
                        null
                }

            "device" ->

                if (
                    segments.size ==
                    1 &&
                    isSafeIdentifier(
                        segments.single()
                    )
                ) {

                    ExternalDestination.Device(
                        Uri.decode(
                            segments.single()
                        )
                    )

                } else {
                    null
                }

            "schedules" ->

                if (
                    segments.isEmpty()
                ) {
                    ExternalDestination.Schedules
                } else {
                    null
                }

            "rules" ->

                if (
                    segments.isEmpty()
                ) {
                    ExternalDestination.Rules
                } else {
                    null
                }

            "settings" ->

                if (
                    segments.isEmpty()
                ) {
                    ExternalDestination.Settings
                } else {
                    null
                }

            else ->
                null
        }
    }

    private fun isSafeIdentifier(
        encoded: String
    ): Boolean {

        val decoded =
            Uri.decode(
                encoded
            )

        if (
            decoded.isBlank() ||
            decoded.length >
            256
        ) {
            return false
        }

        /*
         * PDIDs are opaque to Android, but route separators/control
         * characters must not be allowed to alter navigation grammar.
         */
        return decoded.none {
            character ->

            character ==
                '/' ||
                character ==
                '\\' ||
                character ==
                '\u0000' ||
                character ==
                '\n' ||
                character ==
                '\r'
        }
    }
}
