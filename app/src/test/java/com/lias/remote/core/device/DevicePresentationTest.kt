// ====================================================================
// File: app/src/test/java/com/lias/remote/core/device/DevicePresentationTest.kt
// Version: 19.0.0
//
// Purpose:
//   Regression tests for device grouping, generic normalization,
//   infrastructure precedence and discovery presentation.
// ====================================================================

package com.lias.remote.core.device

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.SourceMeta
import com.lias.remote.core.models.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePresentationTest {

    private val tags =
        listOf(
            Tag(
                id =
                    "infrastructure",
                name =
                    "Infrastructure",
                color =
                    "#8e8e93",
                precedence =
                    100,
                builtin =
                    true
            ),
            Tag(
                id =
                    "kids",
                name =
                    "Kids Devices",
                color =
                    "#ff9500",
                precedence =
                    80,
                builtin =
                    true
            ),
            Tag(
                id =
                    "gaming",
                name =
                    "Gaming Consoles",
                color =
                    "#ff2d55",
                precedence =
                    70,
                builtin =
                    true
            ),
            Tag(
                id =
                    "generic",
                name =
                    "Generic Devices",
                color =
                    "#636366",
                precedence =
                    0,
                builtin =
                    true
            )
        )

    @Test
    fun `empty tags normalize to generic`() {

        val device =
            Device(
                pdid =
                    "a",
                tags =
                    emptyList()
            )

        assertEquals(
            listOf(
                "generic"
            ),
            DevicePresentation
                .normalizedTagIds(
                    device
                )
        )
    }

    @Test
    fun `generic is removed beside meaningful tags`() {

        val device =
            Device(
                pdid =
                    "a",
                tags =
                    listOf(
                        "generic",
                        "kids"
                    )
            )

        assertEquals(
            listOf(
                "kids"
            ),
            DevicePresentation
                .normalizedTagIds(
                    device
                )
        )
    }

    @Test
    fun `highest precedence tag becomes presentation group`() {

        val device =
            Device(
                pdid =
                    "a",
                tags =
                    listOf(
                        "gaming",
                        "kids"
                    )
            )

        assertEquals(
            "kids",
            DevicePresentation
                .primaryTag(
                    device,
                    tags
                )
                ?.id
        )
    }

    @Test
    fun `infrastructure always becomes primary presentation group`() {

        val device =
            Device(
                pdid =
                    "router",
                tags =
                    listOf(
                        "kids",
                        "infrastructure"
                    )
            )

        assertEquals(
            "infrastructure",
            DevicePresentation
                .primaryTag(
                    device,
                    tags
                )
                ?.id
        )
    }

    @Test
    fun `multi tag device appears only once in grouped inventory`() {

        val device =
            Device(
                pdid =
                    "console",
                friendlyName =
                    "Console",
                tags =
                    listOf(
                        "kids",
                        "gaming"
                    )
            )

        val groups =
            DevicePresentation
                .groupDevicesOnce(
                    listOf(
                        device
                    ),
                    tags
                )

        assertEquals(
            1,
            groups.sumOf {
                it.devices.size
            }
        )

        assertEquals(
            "kids",
            groups.single()
                .tag.id
        )
    }

    @Test
    fun `search includes historical mac`() {

        val device =
            Device(
                pdid =
                    "a",
                macs =
                    listOf(
                        "aa:bb:cc:dd:ee:ff"
                    )
            )

        assertTrue(
            DevicePresentation
                .matchesSearch(
                    device,
                    "cc:dd"
                )
        )
    }

    @Test
    fun `search includes services`() {

        val device =
            Device(
                pdid =
                    "a",
                services =
                    listOf(
                        "_airplay._tcp"
                    )
            )

        assertTrue(
            DevicePresentation
                .matchesSearch(
                    device,
                    "airplay"
                )
        )
    }

    @Test
    fun `unrelated search does not match`() {

        val device =
            Device(
                pdid =
                    "a",
                friendlyName =
                    "Kitchen Speaker"
            )

        assertFalse(
            DevicePresentation
                .matchesSearch(
                    device,
                    "playstation"
                )
        )
    }

    @Test
    fun `fractional confidence formats as percentage`() {

        assertEquals(
            "85%",
            DevicePresentation
                .confidencePercent(
                    0.85
                )
        )
    }

    @Test
    fun `percentage confidence remains percentage`() {

        assertEquals(
            "85%",
            DevicePresentation
                .confidencePercent(
                    85.0
                )
        )
    }

    @Test
    fun `source summary preserves discovery source`() {

        val meta =
            SourceMeta(
                source =
                    "ssdp",
                confidence =
                    0.85,
                timestamp =
                    "2026-08-08T12:00:00Z"
            )

        assertEquals(
            "ssdp · 85%",
            DevicePresentation
                .sourceSummary(
                    meta
                )
        )
    }

    @Test
    fun `friendly name remains primary Android display name`() {

        val device =
            Device(
                pdid =
                    "a",
                friendlyName =
                    "Living Room TV",
                hostname =
                    "bravia.local",
                vendor =
                    "Sony"
            )

        assertEquals(
            "Living Room TV",
            device.displayName
        )
    }
}
