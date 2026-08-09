// ====================================================================
// File:
// app/src/test/java/com/lias/remote/ui/access/AccessPresentationTest.kt
// Version: 26.0.0
//
// Purpose:
//   Protect the final screen/action contract.
//
// If these tests regress, UI can again expose actions that the server
// says are invalid.
// ====================================================================

package com.lias.remote.ui.access

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.ExtensionInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessPresentationTest {

    @Test
    fun `infrastructure device is always protected`() {

        val device =
            Device(
                pdid =
                    "router",
                tags =
                    listOf(
                        "infrastructure",
                        "servers"
                    )
            )

        val status =
            EffectiveStatus(
                action =
                    "allow",
                source =
                    "global",
                extendAvailable =
                    true,
                pauseAvailable =
                    true
            )

        val presentation =
            AccessPresentationResolver
                .resolve(
                    device,
                    status
                )

        assertEquals(
            AccessKind.INFRASTRUCTURE,
            presentation.kind
        )

        assertFalse(
            presentation.canPause
        )

        assertFalse(
            presentation.canExtend
        )
    }

    @Test
    fun `missing status exposes no Internet mutation`() {

        val presentation =
            AccessPresentationResolver
                .resolve(
                    Device(
                        pdid =
                            "phone"
                    ),
                    null
                )

        assertEquals(
            AccessKind.UNKNOWN,
            presentation.kind
        )

        assertFalse(
            presentation.canPause
        )

        assertFalse(
            presentation.canExtend
        )

        assertFalse(
            presentation.canResumePause
        )
    }

    @Test
    fun `blocked status exposes extend only when server allows it`() {

        val presentation =
            AccessPresentationResolver
                .resolve(
                    Device(
                        pdid =
                            "tablet"
                    ),
                    EffectiveStatus(
                        action =
                            "block",
                        source =
                            "schedule",
                        extendAvailable =
                            true,
                        pauseAvailable =
                            false
                    )
                )

        assertEquals(
            AccessKind.BLOCKED,
            presentation.kind
        )

        assertTrue(
            presentation.canExtend
        )

        assertFalse(
            presentation.canPause
        )
    }

    @Test
    fun `global kill switch can remain blocked with no extend action`() {

        val presentation =
            AccessPresentationResolver
                .resolve(
                    Device(
                        pdid =
                            "tablet"
                    ),
                    EffectiveStatus(
                        action =
                            "block",
                        source =
                            "global",
                        extendAvailable =
                            false,
                        pauseAvailable =
                            false
                    )
                )

        assertEquals(
            AccessKind.BLOCKED,
            presentation.kind
        )

        assertFalse(
            presentation.canExtend
        )
    }

    @Test
    fun `allowed status exposes pause only when server permits`() {

        val presentation =
            AccessPresentationResolver
                .resolve(
                    Device(
                        pdid =
                            "console"
                    ),
                    EffectiveStatus(
                        action =
                            "allow",
                        source =
                            "tag_policy",
                        extendAvailable =
                            false,
                        pauseAvailable =
                            true
                    )
                )

        assertEquals(
            AccessKind.ALLOWED,
            presentation.kind
        )

        assertTrue(
            presentation.canPause
        )

        assertFalse(
            presentation.canExtend
        )
    }

    @Test
    fun `active pause is identified by reason tag`() {

        val presentation =
            AccessPresentationResolver
                .resolve(
                    Device(
                        pdid =
                            "console"
                    ),
                    EffectiveStatus(
                        action =
                            "block",
                        source =
                            "device_policy",
                        extendAvailable =
                            true,
                        pauseAvailable =
                            false,
                        activeExtension =
                            ExtensionInfo(
                                expiresAt =
                                    "2026-08-08T23:00:00Z",
                                minutesLeft =
                                    42,
                                reasonTag =
                                    "pause"
                            )
                    )
                )

        assertEquals(
            AccessKind.PAUSED,
            presentation.kind
        )

        assertTrue(
            presentation.canResumePause
        )

        assertTrue(
            presentation.isPaused
        )
    }

    @Test
    fun `active extend is manageable`() {

        val presentation =
            AccessPresentationResolver
                .resolve(
                    Device(
                        pdid =
                            "tablet"
                    ),
                    EffectiveStatus(
                        action =
                            "allow",
                        source =
                            "device_policy",
                        extendAvailable =
                            false,
                        pauseAvailable =
                            true,
                        activeExtension =
                            ExtensionInfo(
                                expiresAt =
                                    "2026-08-08T23:00:00Z",
                                minutesLeft =
                                    25,
                                reasonTag =
                                    "extend_access"
                            )
                    )
                )

        assertEquals(
            AccessKind.EXTENDED,
            presentation.kind
        )

        assertTrue(
            presentation.canManageExtension
        )

        assertTrue(
            presentation.canPause
        )
    }

    @Test
    fun `policy naming convention has no influence on presentation`() {

        /*
         * There is intentionally no Policy argument anywhere in
         * AccessPresentationResolver.
         *
         * Pause therefore cannot be inferred from pol_pause_<pdid>.
         */
        val presentation =
            AccessPresentationResolver
                .resolve(
                    Device(
                        pdid =
                            "device_a"
                    ),
                    EffectiveStatus(
                        action =
                            "allow",
                        source =
                            "global",
                        pauseAvailable =
                            true
                    )
                )

        assertEquals(
            AccessKind.ALLOWED,
            presentation.kind
        )

        assertFalse(
            presentation.isPaused
        )
    }
}
