
// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/ExtendHelper.kt
// Version: 1.1.0
// Audit Fixes:
//   1. Added robust multi-format RFC3339/ISO-8601 parsing fallback using
//      Instant.parse, OffsetDateTime.parse, and ZonedDateTime.parse to ensure
//      Go RFC3339 timestamps accurately yield remaining minutes.
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.EffectiveStatus
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import kotlin.math.max

object ExtendHelper {

    fun isExtendAvailable(effectiveStatus: EffectiveStatus?): Boolean {
        if (effectiveStatus == null) return false
        return effectiveStatus.action == "block" && effectiveStatus.extendAvailable
    }

    fun minutesUntil(expiresAtIso: String?): Int {
        if (expiresAtIso.isNullOrBlank()) return 0
        return try {
            val expiryEpoch = try {
                Instant.parse(expiresAtIso).epochSecond
            } catch (_: Exception) {
                try {
                    OffsetDateTime.parse(expiresAtIso).toEpochSecond()
                } catch (_: Exception) {
                    ZonedDateTime.parse(expiresAtIso).toEpochSecond()
                }
            }
            val nowEpoch = Instant.now().epochSecond
            val secondsLeft = expiryEpoch - nowEpoch
            max(0, (secondsLeft / 60).toInt())
        } catch (_: Exception) {
            0
        }
    }
}
