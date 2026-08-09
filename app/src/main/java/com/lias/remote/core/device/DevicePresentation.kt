// ====================================================================
// File: app/src/main/java/com/lias/remote/core/device/DevicePresentation.kt
// Version: 19.0.0
//
// Purpose:
//   Presentation-only classification and evidence formatting.
//
// Critical distinction:
//   primaryTag() decides ONLY where a multi-tag device appears in the
//   Devices screen.
//
//   It does NOT alter device.tags or LIAS policy evaluation.
// ====================================================================

package com.lias.remote.core.device

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.SourceMeta
import com.lias.remote.core.models.Tag
import kotlin.math.roundToInt

data class DeviceGroupPresentation(
    val tag: Tag,
    val devices: List<Device>
)

object DevicePresentation {

    const val INFRASTRUCTURE =
        "infrastructure"

    const val GENERIC =
        "generic"

    fun normalizedTagIds(
        device: Device
    ): List<String> {

        val tags =
            device.safeTags
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .toMutableList()

        if (
            tags.isEmpty()
        ) {
            return listOf(
                GENERIC
            )
        }

        /*
         * generic represents lack of classification.
         * It is not useful next to a more specific classification.
         */
        if (
            tags.size >
                1
        ) {
            tags.remove(
                GENERIC
            )
        }

        return tags.ifEmpty {
            listOf(
                GENERIC
            )
        }
    }

    fun primaryTag(
        device: Device,
        tags: List<Tag>
    ): Tag? {

        val assigned =
            normalizedTagIds(
                device
            )

        return tags
            .filter {
                it.id in assigned
            }
            .maxWithOrNull(
                compareBy<Tag> {
                    it.precedence
                }
                    .thenBy {
                        it.id !=
                            INFRASTRUCTURE
                    }
            )
            ?: tags.find {
                it.id ==
                    GENERIC
            }
    }

    fun groupDevicesOnce(
        devices: List<Device>,
        tags: List<Tag>
    ): List<DeviceGroupPresentation> {

        val grouped =
            linkedMapOf<
                String,
                MutableList<Device>
            >()

        devices.forEach {
                device ->

            val primary =
                primaryTag(
                    device,
                    tags
                )

            val id =
                primary?.id
                    ?: GENERIC

            grouped
                .getOrPut(
                    id
                ) {
                    mutableListOf()
                }
                .add(
                    device
                )
        }

        val tagMap =
            tags.associateBy {
                it.id
            }

        return grouped
            .mapNotNull {
                    (tagId, members) ->

                val tag =
                    tagMap[
                        tagId
                    ]
                        ?: if (
                            tagId ==
                            GENERIC
                        ) {
                            Tag(
                                id =
                                    GENERIC,
                                name =
                                    "Generic Devices",
                                color =
                                    "#636366",
                                precedence =
                                    0,
                                builtin =
                                    true
                            )
                        } else {
                            null
                        }

                tag?.let {
                    DeviceGroupPresentation(
                        tag =
                            it,
                        devices =
                            members
                                .distinctBy {
                                    device ->
                                    device.pdid
                                }
                                .sortedWith(
                                    compareByDescending<Device> {
                                        it.online
                                    }.thenBy {
                                        it.displayName
                                            .lowercase()
                                    }
                                )
                    )
                }
            }
            .sortedWith(
                compareByDescending<
                    DeviceGroupPresentation
                > {
                    it.tag.precedence
                }
                    .thenBy {
                        it.tag.name
                            .lowercase()
                    }
            )
    }

    fun matchesSearch(
        device: Device,
        rawQuery: String
    ): Boolean {

        val query =
            rawQuery
                .trim()
                .lowercase()

        if (
            query.isBlank()
        ) {
            return true
        }

        return sequenceOf(
            device.displayName,
            device.friendlyName,
            device.hostname,
            device.canonicalHostname,
            device.currentMAC,
            device.currentIP,
            device.vendor,
            device.manufacturer,
            device.model,
            device.deviceType,
            device.identityTier,
            device.pdid
        )
            .plus(
                device.safeMacs
                    .asSequence()
            )
            .plus(
                device.safeIps
                    .asSequence()
            )
            .plus(
                device.safeServices
                    .asSequence()
            )
            .any {
                it.lowercase()
                    .contains(
                        query
                    )
            }
    }

    fun identityTierTitle(
        tier: String
    ): String =
        when (
            tier.lowercase()
        ) {

            "bia" ->
                "Hardware Identity"

            "l7" ->
                "Network Identity"

            "tentative" ->
                "Tentative Identity"

            else ->
                tier.ifBlank {
                    "Unknown Identity"
                }
                    .replaceFirstChar {
                        it.uppercase()
                    }
        }

    fun identityTierExplanation(
        tier: String
    ): String =
        when (
            tier.lowercase()
        ) {

            "bia" ->
                "DIS has anchored this device to a hardware-level identity."

            "l7" ->
                "DIS has anchored this device using higher-level network identity evidence."

            "tentative" ->
                "DIS is still correlating this device. Its persistent identity may be promoted when stronger evidence appears."

            else ->
                "Identity tier reported by DIS."
        }

    fun confidencePercent(
        confidence: Double
    ): String {

        val normalized =
            when {
                confidence <=
                    1.0 ->
                    confidence * 100.0

                else ->
                    confidence
            }
                .coerceIn(
                    0.0,
                    100.0
                )

        return "${normalized.roundToInt()}%"
    }

    fun sourceTitle(
        field: String
    ): String =
        when (
            field.lowercase()
        ) {

            "hostname" ->
                "Hostname"

            "friendly_name" ->
                "Friendly Name"

            "manufacturer" ->
                "Manufacturer"

            "vendor" ->
                "Vendor"

            "model" ->
                "Model"

            "device_type" ->
                "Device Type"

            else ->
                field
                    .replace(
                        '_',
                        ' '
                    )
                    .replaceFirstChar {
                        it.uppercase()
                    }
        }

    fun sourceSummary(
        meta: SourceMeta
    ): String =
        buildString {

            append(
                meta.source.ifBlank {
                    "Unknown source"
                }
            )

            if (
                meta.confidence >
                0.0
            ) {

                append(" · ")

                append(
                    confidencePercent(
                        meta.confidence
                    )
                )
            }
        }

    fun deviceTypeTitle(
        device: Device
    ): String =
        device.deviceType
            .ifBlank {
                "Unclassified"
            }
            .replace(
                '_',
                ' '
            )
            .replaceFirstChar {
                it.uppercase()
            }

    fun tagNames(
        device: Device,
        tags: List<Tag>
    ): List<String> {

        val map =
            tags.associateBy {
                it.id
            }

        return normalizedTagIds(
            device
        )
            .map {
                id ->

                map[
                    id
                ]?.name
                    ?: id
                        .replace(
                            '_',
                            ' '
                        )
                        .replaceFirstChar {
                            it.uppercase()
                        }
            }
    }
}
