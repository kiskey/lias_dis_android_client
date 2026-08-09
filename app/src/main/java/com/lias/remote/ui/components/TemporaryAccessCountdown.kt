// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/TemporaryAccessCountdown.kt
// Version: 11.0.0
//
// Purpose:
//   Display a locally ticking representation of a server-owned
//   temporary-access expiry.
//
// Source of truth:
//   ExtensionInfo.expiresAt returned by LIAS.
//
// Important:
//   The Android timer is presentation only.
//   Enforcement and expiry are always maintained by LIAS.
//
// Behavior:
//   - Recomputes from expires_at instead of decrementing mutable state.
//   - Handles process sleep / backgrounding correctly.
//   - Never reports a negative remaining duration.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lias.remote.core.models.ExtensionInfo
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun rememberTemporaryMinutesLeft(
    extension: ExtensionInfo?
): Int? {

    var minutesLeft by
        remember(
            extension?.expiresAt,
            extension?.minutesLeft
        ) {
            mutableIntStateOf(
                calculateMinutesLeft(
                    extension
                )
            )
        }

    LaunchedEffect(
        extension?.expiresAt,
        extension?.minutesLeft
    ) {

        if (
            extension == null
        ) {
            minutesLeft = 0
            return@LaunchedEffect
        }

        while (true) {

            minutesLeft =
                calculateMinutesLeft(
                    extension
                )

            if (
                minutesLeft <= 0
            ) {
                break
            }

            /*
             * Minute-level server semantics do not require a
             * second-by-second visual timer.
             *
             * Thirty seconds keeps the UI fresh while avoiding
             * needless Compose wakeups.
             */
            delay(
                30_000L
            )
        }
    }

    return if (
        extension == null
    ) {
        null
    } else {
        minutesLeft
    }
}

fun calculateMinutesLeft(
    extension: ExtensionInfo?
): Int {

    if (
        extension == null
    ) {
        return 0
    }

    val serverMinutes =
        extension.minutesLeft
            .coerceAtLeast(
                0
            )

    val expiresAt =
        extension.expiresAt
            .trim()

    if (
        expiresAt.isBlank()
    ) {
        return serverMinutes
    }

    return try {

        val expiry =
            Instant.parse(
                expiresAt
            )

        val seconds =
            Duration.between(
                Instant.now(),
                expiry
            )
                .seconds

        if (
            seconds <= 0
        ) {
            0
        } else {

            /*
             * Round upward.
             *
             * 1 second remaining should still display "1 min",
             * rather than prematurely displaying zero.
             */
            (
                (
                    seconds + 59
                    ) / 60
                )
                .toInt()
                .coerceAtLeast(
                    0
                )
        }

    } catch (
        _: Exception
    ) {

        /*
         * Fall back to the server-computed minutes_left if an older
         * LIAS release sends a date representation Android cannot
         * parse.
         */
        serverMinutes
    }
}

fun formatTemporaryDuration(
    minutes: Int
): String {

    val safeMinutes =
        minutes.coerceAtLeast(
            0
        )

    return when {

        safeMinutes <= 0 ->
            "Ending now"

        safeMinutes < 60 ->
            "$safeMinutes min"

        safeMinutes == 60 ->
            "1 hr"

        safeMinutes % 60 == 0 ->
            "${safeMinutes / 60} hr"

        else -> {

            val hours =
                safeMinutes / 60

            val remainingMinutes =
                safeMinutes % 60

            "${hours}h ${remainingMinutes}m"
        }
    }
}
