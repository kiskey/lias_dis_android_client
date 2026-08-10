package com.lias.remote.ui.screens.home

import com.lias.remote.core.device.DevicePresentation
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag

internal enum class HomeTagIconKind {
    SHIELD,
    LOCK,
    HOUSE,
    IPHONE,
    GEAR,
    CLOCK,
    PENCIL
}

private val customIconKinds =
    listOf(
        HomeTagIconKind.SHIELD,
        HomeTagIconKind.HOUSE,
        HomeTagIconKind.IPHONE,
        HomeTagIconKind.GEAR,
        HomeTagIconKind.CLOCK,
        HomeTagIconKind.PENCIL
    )

internal fun Tag?.homeIconKind(): HomeTagIconKind {
    if (this == null) {
        return HomeTagIconKind.SHIELD
    }

    val normalizedId = id.trim().lowercase()
    val searchable = "$normalizedId ${name.trim().lowercase()}"

    return when {
        normalizedId in setOf("infrastructure", "servers") ||
            searchable.contains(" server") ||
            searchable.contains(" nas") ->
            HomeTagIconKind.LOCK

        normalizedId == "work" ||
            searchable.contains("office") ||
            searchable.contains("study") ->
            HomeTagIconKind.PENCIL

        normalizedId in setOf("kids", "guests", "smart_home") ||
            searchable.contains("family") ||
            searchable.contains(" home") ->
            HomeTagIconKind.HOUSE

        normalizedId in setOf("mobile", "streaming", "audio") ||
            searchable.contains("phone") ||
            searchable.contains("tablet") ||
            searchable.contains("media") ->
            HomeTagIconKind.IPHONE

        normalizedId in setOf("gaming", "computers", "iot") ||
            searchable.contains("game") ||
            searchable.contains("computer") ||
            searchable.contains("appliance") ->
            HomeTagIconKind.GEAR

        normalizedId == "printers" ||
            searchable.contains("print") ||
            searchable.contains("scanner") ->
            HomeTagIconKind.PENCIL

        normalizedId == "generic" ->
            HomeTagIconKind.SHIELD

        else ->
            customIconKinds[
                Math.floorMod(
                    normalizedId.ifBlank { name }.hashCode(),
                    customIconKinds.size
                )
            ]
    }
}

internal fun Device.homePrimaryTag(
    tags: List<Tag>
): Tag? =
    DevicePresentation.primaryTag(this, tags)
