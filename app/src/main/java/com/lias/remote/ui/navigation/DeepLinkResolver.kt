// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/DeepLinkResolver.kt
// Version: 3.0.0
//
// Purpose:
//   Parse external Android intents into safe internal LIAS routes.
//
// Supported:
//   lias://device/device/<pdid>
//   https://lias.local/device/<pdid>
//
// Design:
//   Deep links are resolved once at the navigation boundary.
//   Screens never parse Intent or Uri objects themselves.
// ====================================================================

package com.lias.remote.ui.navigation

import android.content.Intent
import android.net.Uri

sealed interface LiasDeepLink {

    data class Device(
        val pdid: String
    ) : LiasDeepLink

    data object Home : LiasDeepLink

    data object Devices : LiasDeepLink
}

object DeepLinkResolver {

    fun resolve(
        intent: Intent?
    ): LiasDeepLink? {

        val uri =
            intent?.data
                ?: return null

        return resolve(
            uri
        )
    }

    fun resolve(
        uri: Uri
    ): LiasDeepLink? {

        val scheme =
            uri.scheme
                ?.lowercase()
                ?: return null

        val host =
            uri.host
                ?.lowercase()
                ?: return null

        return when {

            scheme == LiasRoute.SCHEME &&
                host == LiasRoute.HOST ->
                resolveLiasScheme(
                    uri
                )

            scheme == LiasRoute.WEB_SCHEME &&
                host == LiasRoute.WEB_HOST ->
                resolveWebScheme(
                    uri
                )

            else ->
                null
        }
    }

    private fun resolveLiasScheme(
        uri: Uri
    ): LiasDeepLink? {

        val segments =
            uri.pathSegments

        if (segments.size != 2) {
            return null
        }

        if (
            !segments[0].equals(
                "device",
                ignoreCase = true
            )
        ) {
            return null
        }

        return segments[1]
            .trim()
            .takeIf {
                it.isNotBlank()
            }
            ?.let {
                LiasDeepLink.Device(
                    pdid = it
                )
            }
    }

    private fun resolveWebScheme(
        uri: Uri
    ): LiasDeepLink? {

        val segments =
            uri.pathSegments

        if (segments.size != 2) {
            return null
        }

        if (
            !segments[0].equals(
                "device",
                ignoreCase = true
            )
        ) {
            return null
        }

        return segments[1]
            .trim()
            .takeIf {
                it.isNotBlank()
            }
            ?.let {
                LiasDeepLink.Device(
                    pdid = it
                )
            }
    }
}
