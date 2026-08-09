// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/connect/ConnectionStrings.kt
// Version: 4.0.0
//
// Purpose:
//   Centralized user-facing connection copy.
//
// This keeps connection wording consistent between onboarding and
// Settings without coupling those screens together.
// ====================================================================

package com.lias.remote.ui.screens.connect

object ConnectionStrings {

    const val TITLE =
        "Connect to LIAS"

    const val DESCRIPTION =
        "Enter your home server address to manage devices, schedules and rules."

    const val SERVER_URL_LABEL =
        "Server URL"

    const val SERVER_URL_PLACEHOLDER =
        "http://192.168.1.1:8081"

    const val AUTH_TOKEN_LABEL =
        "Auth Token (Optional)"

    const val CONNECT =
        "Connect"

    const val CHECKING =
        "Checking Server…"

    const val CONNECTING =
        "Connecting…"

    const val TEST_CONNECTION =
        "Test Connection"

    const val TESTING =
        "Testing…"

    const val APPLYING =
        "Applying…"

    const val VERIFIED =
        "Connection verified"

    const val INVALID_SERVER =
        "Enter a valid LIAS server address."

    const val UNREACHABLE =
        "Unable to reach the LIAS server."

    const val NOT_LIAS_SERVER =
        "This does not appear to be a LIAS server."

    const val AUTH_REQUIRED =
        "The server requires a valid authentication token."

    const val ACCESS_DENIED =
        "The server denied access."
}
