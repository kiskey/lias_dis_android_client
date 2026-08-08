// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/NavigationRoutes.kt
// Version: 20.0.0
//
// Purpose:
//   Central navigation and external-deep-link contract.
//
// Supported external URI examples:
//
//   liasremote://home
//   liasremote://devices
//   liasremote://device/<pdid>
//   liasremote://devices/<pdid>
//   liasremote://schedules
//   liasremote://rules
//   liasremote://settings
//
// Security:
//   - Deep links NEVER contain or accept authentication tokens.
//   - Server URL cannot be changed by a deep link.
//   - Unknown URI paths are rejected.
//   - Device PDIDs are URI-decoded only after parsing.
//
// Deep links are navigation hints only. They do not bypass:
//   - configuration gate
//   - authentication
//   - infrastructure protection
//   - LIAS authorization
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

    const val DEVICE_DETAIL =
        "device_detail/{pdid}"

    fun deviceDetail(
        pdid: String
    ): String =
        "device_detail/${Uri.encode(pdid)}"
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

    const val SCHEME =
        "liasremote"

    fun parse(
        rawUri: String?
    ): ExternalDestination? {

        if (
            rawUri.isNullOrBlank()
        ) {
            return null
        }

        val uri =
            try {
                Uri.parse(
                    rawUri
                )
            } catch (
                _: Exception
            ) {
                return null
            }

        if (
            !uri.scheme.equals(
                SCHEME,
                ignoreCase = true
            )
        ) {
            return null
        }

        val host =
            uri.host
                ?.trim()
                ?.lowercase()
                .orEmpty()

        val pathSegments =
            uri.pathSegments
                .filter {
                    it.isNotBlank()
                }

        return when (
            host
        ) {

            "home" -> {

                if (
                    pathSegments.isEmpty()
                ) {
                    ExternalDestination.Home
                } else {
                    null
                }
            }

            "devices" -> {

                when (
                    pathSegments.size
                ) {

                    0 ->
                        ExternalDestination.Devices

                    1 ->
                        decodeDevice(
                            pathSegments[
                                0
                            ]
                        )

                    else ->
                        null
                }
            }

            "device" -> {

                if (
                    pathSegments.size ==
                    1
                ) {
                    decodeDevice(
                        pathSegments[
                            0
                        ]
                    )
                } else {
                    null
                }
            }

            "schedules" -> {

                if (
                    pathSegments.isEmpty()
                ) {
                    ExternalDestination.Schedules
                } else {
                    null
                }
            }

            "rules" -> {

                if (
                    pathSegments.isEmpty()
                ) {
                    ExternalDestination.Rules
                } else {
                    null
                }
            }

            "settings" -> {

                if (
                    pathSegments.isEmpty()
                ) {
                    ExternalDestination.Settings
                } else {
                    null
                }
            }

            else ->
                null
        }
    }

    private fun decodeDevice(
        encoded: String
    ): ExternalDestination.Device? {

        val pdid =
            try {
                Uri.decode(
                    encoded
                )
                    .trim()
            } catch (
                _: Exception
            ) {
                ""
            }

        if (
            pdid.isBlank() ||
            pdid.length >
            512
        ) {
            return null
        }

        return ExternalDestination.Device(
            pdid =
                pdid
        )
    }
}
