package com.lias.remote.ui.screens.home

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.EffectiveStatus
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Tag
import com.lias.remote.repositories.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePresentationTest {

    @Test
    fun `restricted devices use authoritative paused and blocked states only`() {
        val state =
            UiState(
                devices =
                    listOf(
                        device("allowed", "Allowed"),
                        device("blocked", "Blocked"),
                        device("paused", "Paused"),
                        device("extended", "Extended"),
                        device("global", "Global")
                    ),
                deviceEffectiveStatuses =
                    mapOf(
                        "allowed" to status("allow", "fallback"),
                        "blocked" to status("block", "device_policy"),
                        "paused" to
                            status(
                                action = "block",
                                source = "device_policy",
                                extension = ExtensionInfo(reasonTag = "pause")
                            ),
                        "extended" to
                            status(
                                action = "allow",
                                source = "device_policy",
                                extension = ExtensionInfo(reasonTag = "extend_access")
                            ),
                        "global" to status("block", "global")
                    )
            )

        assertEquals(
            listOf("paused", "blocked"),
            state.homeRestrictedDevices().map { it.pdid }
        )
    }

    @Test
    fun `active tag protections exclude duplicate and fallback branches`() {
        val tags =
            listOf(
                tag("kids", "Kids"),
                tag("study", "Study"),
                tag("global", "Global duplicate"),
                tag("fallback", "Fallback"),
                tag("infra", "Infrastructure")
            )
        val state =
            UiState(
                tags = tags,
                policies =
                    listOf(
                        tagPolicy("kids"),
                        tagPolicy("study"),
                        tagPolicy("global"),
                        tagPolicy("fallback"),
                        tagPolicy("infra")
                    ),
                tagEffectiveStatuses =
                    mapOf(
                        "kids" to status("block", "tag_policy"),
                        "study" to status("allow", "schedule"),
                        "global" to status("block", "global"),
                        "fallback" to status("allow", "fallback"),
                        "infra" to status("allow", "infrastructure")
                    )
            )

        assertEquals(
            listOf("kids"),
            state.homeActiveTagProtections().map { it.tag.id }
        )
    }

    @Test
    fun `active tag protections require an eligible blocking policy for the group`() {
        val state =
            UiState(
                tags = listOf(tag("kids", "Kids"), tag("unused", "Unused")),
                policies =
                    listOf(
                        tagPolicy("kids"),
                        tagPolicy("unused", action = "allow")
                    ),
                tagEffectiveStatuses =
                    mapOf(
                        "kids" to status("block", "tag_policy"),
                        "unused" to status("block", "schedule")
                    )
            )

        assertEquals(
            listOf("kids"),
            state.homeActiveTagProtections().map { it.tag.id }
        )
    }

    @Test
    fun `active pause list contains only server identified temporary pauses`() {
        val state =
            UiState(
                devices =
                    listOf(
                        device("paused", "Paused"),
                        device("blocked", "Blocked")
                    ),
                deviceEffectiveStatuses =
                    mapOf(
                        "paused" to
                            status(
                                "block",
                                "device_policy",
                                ExtensionInfo(reasonTag = "pause")
                            ),
                        "blocked" to status("block", "device_policy")
                    )
            )

        assertEquals(
            listOf("paused"),
            state.homeActivePauseDevices().map { it.pdid }
        )
    }

    @Test
    fun `global protection is shown once only for authoritative overrides`() {
        val allow = globalPolicy("allow")
        val schedule = globalPolicy("schedule")
        val loaded = UiState(policies = listOf(allow), isInitialLoaded = true)

        assertTrue(loaded.homeHasGlobalProtection(allow))
        assertFalse(loaded.homeHasGlobalProtection(schedule))
        assertFalse(
            UiState(policies = listOf(allow)).homeHasGlobalProtection(allow)
        )
    }

    private fun device(pdid: String, name: String) =
        Device(pdid = pdid, friendlyName = name)

    private fun status(
        action: String,
        source: String,
        extension: ExtensionInfo? = null
    ) =
        EffectiveStatus(
            action = action,
            source = source,
            activeExtension = extension
        )

    private fun tag(id: String, name: String) =
        Tag(id = id, name = name, color = "", precedence = 0, builtin = false)

    private fun globalPolicy(action: String) =
        Policy(
            id = "global_default",
            name = "Global Access",
            type = "global",
            action = action
        )

    private fun tagPolicy(
        targetId: String,
        action: String = "schedule"
    ) =
        Policy(
            id = "policy_$targetId",
            name = "Policy $targetId",
            type = "tag",
            targetID = targetId,
            action = action
        )
}
