package com.lias.remote.ui.screens.home

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTagIconsTest {

    @Test
    fun `built in tag icon mappings are semantic`() {
        assertEquals(HomeTagIconKind.LOCK, tag("infrastructure").homeIconKind())
        assertEquals(HomeTagIconKind.PENCIL, tag("work").homeIconKind())
        assertEquals(HomeTagIconKind.HOUSE, tag("kids").homeIconKind())
        assertEquals(HomeTagIconKind.GEAR, tag("gaming").homeIconKind())
        assertEquals(HomeTagIconKind.IPHONE, tag("streaming").homeIconKind())
        assertEquals(HomeTagIconKind.IPHONE, tag("mobile").homeIconKind())
        assertEquals(HomeTagIconKind.IPHONE, tag("audio").homeIconKind())
        assertEquals(HomeTagIconKind.GEAR, tag("computers").homeIconKind())
        assertEquals(HomeTagIconKind.HOUSE, tag("smart_home").homeIconKind())
        assertEquals(HomeTagIconKind.GEAR, tag("iot").homeIconKind())
        assertEquals(HomeTagIconKind.PENCIL, tag("printers").homeIconKind())
        assertEquals(HomeTagIconKind.LOCK, tag("servers").homeIconKind())
        assertEquals(HomeTagIconKind.HOUSE, tag("guests").homeIconKind())
        assertEquals(HomeTagIconKind.SHIELD, tag("generic").homeIconKind())
    }

    @Test
    fun `custom tag icon mapping is deterministic and bounded`() {
        val custom = tag("weekend_lab", builtin = false)
        val first = custom.homeIconKind()

        assertEquals(first, custom.homeIconKind())
        assertTrue(first != HomeTagIconKind.LOCK)
        assertNotEquals(HomeTagIconKind.LOCK, tag("camera_zone", false).homeIconKind())
    }

    @Test
    fun `device icon follows its highest precedence assigned tag`() {
        val generic = tag("generic", precedence = 0)
        val kids = tag("kids", precedence = 80)
        val device = Device(pdid = "tablet", tags = listOf("generic", "kids"))

        assertEquals(kids, device.homePrimaryTag(listOf(generic, kids)))
        assertEquals(HomeTagIconKind.HOUSE, device.homePrimaryTag(listOf(generic, kids)).homeIconKind())
    }

    private fun tag(
        id: String,
        builtin: Boolean = true,
        precedence: Int = 50
    ) =
        Tag(
            id = id,
            name = id.replace('_', ' '),
            color = "#007AFF",
            precedence = precedence,
            builtin = builtin
        )
}
