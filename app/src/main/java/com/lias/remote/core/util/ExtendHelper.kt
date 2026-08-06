// ====================================================================
// File: app/src/main/java/com/lias/remote/core/util/ExtendHelper.kt
// Version: 1.0.0
// Purpose: Extend Access availability and countdown calculation helpers (§2.6).
// ====================================================================

package com.lias.remote.core.util

import com.lias.remote.core.models.EffectiveStatus
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.max

object ExtendHelper {

    fun isExtendAvailable(effectiveStatus: EffectiveStatus?): Boolean {
        if (effectiveStatus == null) return false
        return effectiveStatus.action == "block" && effectiveStatus.extendAvailable
    }

    fun minutesUntil(expiresAtIso: String): Int {
        if (expiresAtIso.isBlank()) return 0
        return try {
            val expiry = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(expiresAtIso))
            val now = Instant.now()
            val secondsLeft = expiry.epochSecond - now.epochSecond
            max(0, (secondsLeft / 60).toInt())
        } catch (_: Exception) {
            0
        }
    }
}
