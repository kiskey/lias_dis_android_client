// ====================================================================
// File:
// app/src/test/java/com/lias/remote/ui/navigation/NavigationRoutesTest.kt
// Version: 25.0.0
//
// Purpose:
//   Regression protection for Batch 20/24/25 navigation integration.
//
// Requires Robolectric from Batch 23 because android.net.Uri is used.
// ====================================================================

package com.lias.remote.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(
    RobolectricTestRunner::class
)
class NavigationRoutesTest {

    @Test
    fun `home deep link resolves home`() {

        assertEquals(
            ExternalDestination.Home,
            LiasDeepLinks.parse(
                "liasremote://home"
            )
        )
    }

    @Test
    fun `devices deep link resolves devices`() {

        assertEquals(
            ExternalDestination.Devices,
            LiasDeepLinks.parse(
                "liasremote://devices"
            )
        )
    }

    @Test
    fun `device singular route resolves pdid`() {

        val result =
            LiasDeepLinks.parse(
                "liasremote://device/pdid_abc123"
            )

        assertEquals(
            ExternalDestination.Device(
                "pdid_abc123"
            ),
            result
        )
    }

    @Test
    fun `device plural route resolves pdid`() {

        val result =
            LiasDeepLinks.parse(
                "liasremote://devices/pdid_abc123"
            )

        assertEquals(
            ExternalDestination.Device(
                "pdid_abc123"
            ),
            result
        )
    }

    @Test
    fun `device detail builder keeps opaque id`() {

        val route =
            NavigationRoutes
                .deviceDetail(
                    "pdid_abc-123"
                )

        assertTrue(
            route.startsWith(
                "device_detail/"
            )
        )

        assertTrue(
            route.contains(
                "pdid_abc-123"
            )
        )
    }

    @Test
    fun `schedules route resolves`() {

        assertEquals(
            ExternalDestination.Schedules,
            LiasDeepLinks.parse(
                "liasremote://schedules"
            )
        )
    }

    @Test
    fun `rules route resolves`() {

        assertEquals(
            ExternalDestination.Rules,
            LiasDeepLinks.parse(
                "liasremote://rules"
            )
        )
    }

    @Test
    fun `settings route resolves`() {

        assertEquals(
            ExternalDestination.Settings,
            LiasDeepLinks.parse(
                "liasremote://settings"
            )
        )
    }

    @Test
    fun `foreign scheme rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "https://example.com/devices/test"
            )
        )
    }

    @Test
    fun `unknown host rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "liasremote://something-else"
            )
        )
    }

    @Test
    fun `server configuration via deep link rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "liasremote://settings?server=http://evil.example"
            )
        )
    }

    @Test
    fun `token via deep link rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "liasremote://home?token=secret"
            )
        )
    }

    @Test
    fun `auth token alias via deep link rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "liasremote://devices?auth_token=secret"
            )
        )
    }

    @Test
    fun `route separator injection rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "liasremote://device/one%2Ftwo"
            )
        )
    }

    @Test
    fun `empty device id rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "liasremote://device/"
            )
        )
    }

    @Test
    fun `extra device path rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "liasremote://device/a/b"
            )
        )
    }
}
