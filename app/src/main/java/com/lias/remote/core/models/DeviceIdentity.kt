// ====================================================================
// File: app/src/main/java/com/lias/remote/core/models/DeviceIdentity.kt
// Version: 3.0.0
//
// Purpose:
//   Canonical presentation/identity helpers for Device.
//
// Important:
//   PDID remains the backend identity. This file does NOT invent a
//   replacement Android-side identifier.
//
//   The backend model already provides:
//     - pdid
//     - identity_tier
//     - identity_anchor
//     - current_mac
//     - macs
//     - current_ip
//     - ips
//     - hostname
//     - canonical_hostname
//     - friendly_name
//     - confidence
//
//   These helpers only define how those values should be presented.
// ====================================================================

package com.lias.remote.core.models

enum class IdentityTier(
    val wireValue: String,
    val title: String
) {
    TENTATIVE(
        wireValue = "tentative",
        title = "Tentative"
    ),

    STABLE(
        wireValue = "stable",
        title = "Stable"
    ),

    VERIFIED(
        wireValue = "verified",
        title = "Verified"
    ),

    UNKNOWN(
        wireValue = "",
        title = "Unknown"
    );

    companion object {

        fun fromWireValue(
            value: String?
        ): IdentityTier =
            entries.firstOrNull {
                it.wireValue.equals(
                    value?.trim(),
                    ignoreCase = true
                )
            } ?: UNKNOWN
    }
}

data class DeviceIdentityPresentation(
    val title: String,
    val subtitle: String,
    val identityTier: IdentityTier,
    val confidencePercent: Int,
    val pdid: String,
    val currentMac: String,
    val currentIp: String,
    val isOnline: Boolean
)

object DeviceIdentityFormatter {

    fun present(
        device: Device
    ): DeviceIdentityPresentation {

        val tier =
            IdentityTier.fromWireValue(
                device.identityTier
            )

        val confidence =
            (
                device.confidence
                    .coerceIn(0.0, 1.0) * 100.0
            ).toInt()

        val subtitle =
            when {
                device.online &&
                    device.currentIP.isNotBlank() ->
                    "Online · ${device.currentIP}"

                device.currentIP.isNotBlank() ->
                    device.currentIP

                device.currentMAC.isNotBlank() ->
                    device.currentMAC

                else ->
                    "Offline"
            }

        return DeviceIdentityPresentation(
            title = device.displayName,
            subtitle = subtitle,
            identityTier = tier,
            confidencePercent = confidence,
            pdid = device.pdid,
            currentMac = device.currentMAC,
            currentIp = device.currentIP,
            isOnline = device.online
        )
    }

    /**
     * Human-readable identity label.
     *
     * This deliberately does not expose PDID as the primary label.
     * PDID is an internal/canonical identity and is useful for
     * diagnostics, navigation, and support—not as the everyday title.
     */
    fun identityLabel(
        device: Device
    ): String =
        when {
            device.identityAnchor.isNotBlank() ->
                device.identityAnchor

            device.currentMAC.isNotBlank() ->
                device.currentMAC

            device.pdid.isNotBlank() ->
                device.pdid

            else ->
                "Unknown identity"
        }

    fun identitySummary(
        device: Device
    ): String {

        val tier =
            IdentityTier.fromWireValue(
                device.identityTier
            )

        return when (tier) {
            IdentityTier.VERIFIED ->
                "Verified device identity"

            IdentityTier.STABLE ->
                "Stable device identity"

            IdentityTier.TENTATIVE ->
                "Identity still being confirmed"

            IdentityTier.UNKNOWN ->
                "Identity status unavailable"
        }
    }

    fun accessibilityDescription(
        device: Device
    ): String {

        val status =
            if (device.online) {
                "online"
            } else {
                "offline"
            }

        return buildString {

            append(device.displayName)

            append(", ")
            append(status)

            if (device.currentIP.isNotBlank()) {
                append(", IP ")
                append(device.currentIP)
            }

            if (device.currentMAC.isNotBlank()) {
                append(", MAC ")
                append(device.currentMAC)
            }

            append(", ")
            append(identitySummary(device))
        }
    }
}
