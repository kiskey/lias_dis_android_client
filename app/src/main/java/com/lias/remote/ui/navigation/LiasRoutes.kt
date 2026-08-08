// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/navigation/LiasRoutes.kt
// Version: 3.0.0
//
// Purpose:
//   Canonical navigation contract for LIAS Remote.
//
// Rules:
//   - Route construction happens here only.
//   - Dynamic identifiers are URI encoded.
//   - UI screens must never manually concatenate navigation paths.
//   - External/deep-link URI structure is also defined here.
// ====================================================================

package com.lias.remote.ui.navigation

import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.Gear
import io.github.alexzhirkevich.cupertino.icons.outlined.House
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Shield

sealed class LiasRoute(
    val route: String
) {

    data object Home :
        LiasRoute("home")

    data object Devices :
        LiasRoute("devices")

    data object Schedules :
        LiasRoute("schedules")

    data object Rules :
        LiasRoute("rules")

    data object Settings :
        LiasRoute("settings")

    data object ConnectionSettings :
        LiasRoute("connection_settings")

    data object DeviceDetail :
        LiasRoute("device_detail/{pdid}") {

        const val ARG_PDID = "pdid"

        fun create(
            pdid: String
        ): String =
            "device_detail/${Uri.encode(pdid)}"
    }

    companion object {

        const val SCHEME = "lias"
        const val HOST = "device"

        const val WEB_SCHEME = "https"
        const val WEB_HOST = "lias.local"

        fun deviceDeepLink(
            pdid: String
        ): Uri =
            Uri.Builder()
                .scheme(SCHEME)
                .authority(HOST)
                .appendPath("device")
                .appendPath(pdid)
                .build()

        fun webDeviceDeepLink(
            pdid: String
        ): Uri =
            Uri.Builder()
                .scheme(WEB_SCHEME)
                .authority(WEB_HOST)
                .appendPath("device")
                .appendPath(pdid)
                .build()
    }
}

enum class LiasTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(
        route = LiasRoute.Home.route,
        label = "Home",
        icon = CupertinoIcons.Outlined.House
    ),

    DEVICES(
        route = LiasRoute.Devices.route,
        label = "Devices",
        icon = CupertinoIcons.Outlined.Iphone
    ),

    SCHEDULES(
        route = LiasRoute.Schedules.route,
        label = "Schedules",
        icon = CupertinoIcons.Outlined.Clock
    ),

    RULES(
        route = LiasRoute.Rules.route,
        label = "Rules",
        icon = CupertinoIcons.Outlined.Shield
    ),

    SETTINGS(
        route = LiasRoute.Settings.route,
        label = "Settings",
        icon = CupertinoIcons.Outlined.Gear
    );

    companion object {

        val all: List<LiasTab>
            get() = entries

        fun fromRoute(
            route: String?
        ): LiasTab? =
            entries.firstOrNull {
                it.route == route
            }
    }
}
