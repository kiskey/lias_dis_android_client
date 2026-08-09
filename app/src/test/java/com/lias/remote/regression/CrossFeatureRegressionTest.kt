// ====================================================================
// File:
// app/src/test/java/com/lias/remote/regression/CrossFeatureRegressionTest.kt
// Version: 23.0.0
//
// Purpose:
//   Cross-feature invariants accumulated through Batches 15–22.
//
// Covers:
//   - infrastructure immunity / target exclusion
//   - generic tag normalization
//   - single visual grouping for multi-tag devices
//   - schedule dependency deletion blocking
//   - tag dependency deletion blocking
//   - empty schedule policy default-open semantics
//   - missing schedule rejection
//   - global policy creation restriction
//   - server-owned policy IDs
//   - server-owned schedule IDs
//   - overnight schedule semantics
//   - calendar date validation
//   - diagnostics credential redaction
//   - error taxonomy presentation
//   - configuration gate semantics
//   - navigation/deep-link parsing
//
// Robolectric is used only because LiasDeepLinks relies on android.net.Uri.
// ====================================================================

package com.lias.remote.regression

import com.lias.remote.core.device.DevicePresentation
import com.lias.remote.core.diagnostics.DiagnosticKind
import com.lias.remote.core.diagnostics.ErrorPresentation
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import com.lias.remote.core.models.Tag
import com.lias.remote.core.network.ApiResult
import com.lias.remote.core.policy.PolicyDraft
import com.lias.remote.core.policy.PolicySemantics
import com.lias.remote.core.schedule.ScheduleDraft
import com.lias.remote.core.schedule.ScheduleRuleDraft
import com.lias.remote.core.schedule.ScheduleRuleScope
import com.lias.remote.core.schedule.ScheduleSemantics
import com.lias.remote.core.util.ConfigurationSafety
import com.lias.remote.ui.SettingsUiState
import com.lias.remote.ui.navigation.ExternalDestination
import com.lias.remote.ui.navigation.LiasDeepLinks
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(
    RobolectricTestRunner::class
)
class CrossFeatureRegressionTest {

    private val infrastructureTag =
        Tag(
            id =
                "infrastructure",
            name =
                "Infrastructure",
            color =
                "#8E8E93",
            precedence =
                100,
            builtin =
                true
        )

    private val kidsTag =
        Tag(
            id =
                "kids",
            name =
                "Kids",
            color =
                "#FF9500",
            precedence =
                80,
            builtin =
                true
        )

    private val gamingTag =
        Tag(
            id =
                "gaming",
            name =
                "Gaming",
            color =
                "#FF2D55",
            precedence =
                70,
            builtin =
                true
        )

    private val genericTag =
        Tag(
            id =
                "generic",
            name =
                "Generic",
            color =
                "#636366",
            precedence =
                0,
            builtin =
                true
        )

    private val allTags =
        listOf(
            infrastructureTag,
            kidsTag,
            gamingTag,
            genericTag
        )

    @Test
    fun `infrastructure is excluded from policy tag targets`() {

        val available =
            PolicySemantics
                .availableTags(
                    allTags
                )

        assertFalse(
            available.any {
                it.id ==
                    "infrastructure"
            }
        )
    }

    @Test
    fun `infrastructure devices are excluded from policy device targets`() {

        val router =
            Device(
                pdid =
                    "router",
                friendlyName =
                    "Router",
                tags =
                    listOf(
                        "infrastructure"
                    )
            )

        val tablet =
            Device(
                pdid =
                    "tablet",
                friendlyName =
                    "Tablet",
                tags =
                    listOf(
                        "kids"
                    )
            )

        val available =
            PolicySemantics
                .availableDevices(
                    listOf(
                        router,
                        tablet
                    )
                )

        assertEquals(
            listOf(
                "tablet"
            ),
            available.map {
                it.pdid
            }
        )
    }

    @Test
    fun `generic is removed when meaningful device tags exist`() {

        val device =
            Device(
                pdid =
                    "console",
                tags =
                    listOf(
                        "generic",
                        "kids",
                        "gaming"
                    )
            )

        assertEquals(
            listOf(
                "kids",
                "gaming"
            ),
            DevicePresentation
                .normalizedTagIds(
                    device
                )
        )
    }

    @Test
    fun `multi tag device is rendered once under highest precedence group`() {

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
                    devices =
                        listOf(
                            device
                        ),
                    tags =
                        allTags
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
    fun `infrastructure wins presentation grouping even with other tags`() {

        val device =
            Device(
                pdid =
                    "router",
                tags =
                    listOf(
                        "gaming",
                        "infrastructure",
                        "kids"
                    )
            )

        assertEquals(
            "infrastructure",
            DevicePresentation
                .primaryTag(
                    device,
                    allTags
                )
                ?.id
        )
    }

    @Test
    fun `schedule deletion is blocked while policy references it`() {

        val schedule =
            Schedule(
                id =
                    "sched_bedtime",
                name =
                    "Bedtime",
                mode =
                    "downtime",
                timezone =
                    "UTC",
                rules =
                    listOf(
                        ScheduleRule(
                            days =
                                listOf(
                                    "mon"
                                ),
                            startTime =
                                "22:00",
                            endTime =
                                "06:00",
                            action =
                                "block"
                        )
                    )
            )

        val policy =
            Policy(
                id =
                    "pol_kids",
                name =
                    "Kids Bedtime",
                type =
                    "tag",
                targetID =
                    "kids",
                action =
                    "schedule",
                scheduleIDs =
                    listOf(
                        "sched_bedtime"
                    ),
                priority =
                    50,
                enabled =
                    true
            )

        val impact =
            ConfigurationSafety
                .scheduleImpact(
                    schedule =
                        schedule,
                    policies =
                        listOf(
                            policy
                        )
                )

        assertTrue(
            impact.hasDependencies
        )

        assertFalse(
            impact.canDeleteSafely
        )

        assertEquals(
            "pol_kids",
            impact
                .referencingPolicies
                .single()
                .id
        )
    }

    @Test
    fun `unused schedule can be safely deleted`() {

        val schedule =
            Schedule(
                id =
                    "sched_unused",
                name =
                    "Unused",
                mode =
                    "downtime",
                timezone =
                    "UTC"
            )

        val impact =
            ConfigurationSafety
                .scheduleImpact(
                    schedule,
                    emptyList()
                )

        assertTrue(
            impact.canDeleteSafely
        )

        assertFalse(
            impact.hasDependencies
        )
    }

    @Test
    fun `tag deletion is blocked by assigned device`() {

        val device =
            Device(
                pdid =
                    "tablet",
                tags =
                    listOf(
                        "kids"
                    )
            )

        val impact =
            ConfigurationSafety
                .tagImpact(
                    tag =
                        kidsTag,
                    devices =
                        listOf(
                            device
                        ),
                    policies =
                        emptyList()
                )

        assertFalse(
            impact.canDeleteSafely
        )

        assertEquals(
            "tablet",
            impact
                .assignedDevices
                .single()
                .pdid
        )
    }

    @Test
    fun `tag deletion is blocked by targeting policy`() {

        val policy =
            Policy(
                id =
                    "pol_kids",
                name =
                    "Kids",
                type =
                    "tag",
                targetID =
                    "kids",
                action =
                    "block",
                priority =
                    50,
                enabled =
                    true
            )

        val impact =
            ConfigurationSafety
                .tagImpact(
                    tag =
                        kidsTag,
                    devices =
                        emptyList(),
                    policies =
                        listOf(
                            policy
                        )
                )

        assertFalse(
            impact.canDeleteSafely
        )

        assertEquals(
            "pol_kids",
            impact
                .targetingPolicies
                .single()
                .id
        )
    }

    @Test
    fun `infrastructure tag can never be deleted`() {

        val impact =
            ConfigurationSafety
                .tagImpact(
                    tag =
                        infrastructureTag,
                    devices =
                        emptyList(),
                    policies =
                        emptyList()
                )

        assertTrue(
            impact.isInfrastructure
        )

        assertFalse(
            impact.canDeleteSafely
        )
    }

    @Test
    fun `empty schedule bundle remains valid default open policy`() {

        val result =
            PolicySemantics
                .validateDraft(
                    draft =
                        PolicyDraft(
                            name =
                                "Kids Schedule",
                            type =
                                "tag",
                            targetId =
                                "kids",
                            action =
                                "schedule",
                            scheduleIds =
                                emptySet()
                        ),
                    initialPolicy =
                        null,
                    tags =
                        allTags,
                    devices =
                        emptyList(),
                    schedules =
                        emptyList()
                )

        assertTrue(
            result.valid
        )
    }

    @Test
    fun `missing referenced schedule is rejected client side`() {

        val result =
            PolicySemantics
                .validateDraft(
                    draft =
                        PolicyDraft(
                            name =
                                "Bad Schedule",
                            type =
                                "tag",
                            targetId =
                                "kids",
                            action =
                                "schedule",
                            scheduleIds =
                                setOf(
                                    "sched_deleted"
                                )
                        ),
                    initialPolicy =
                        null,
                    tags =
                        allTags,
                    devices =
                        emptyList(),
                    schedules =
                        emptyList()
                )

        assertFalse(
            result.valid
        )
    }

    @Test
    fun `new global policy cannot be fabricated`() {

        val result =
            PolicySemantics
                .validateDraft(
                    draft =
                        PolicyDraft(
                            name =
                                "Second Global",
                            type =
                                "global",
                            action =
                                "block"
                        ),
                    initialPolicy =
                        null,
                    tags =
                        allTags,
                    devices =
                        emptyList(),
                    schedules =
                        emptyList()
                )

        assertFalse(
            result.valid
        )
    }

    @Test
    fun `new policy id remains empty for LIAS generation`() {

        val policy =
            PolicyDraft(
                name =
                    "Kids Internet",
                type =
                    "tag",
                targetId =
                    "kids",
                action =
                    "block"
            )
                .toPolicy(
                    initialPolicy =
                        null
                )

        assertEquals(
            "",
            policy.id
        )
    }

    @Test
    fun `new schedule id remains empty for LIAS generation`() {

        val schedule =
            ScheduleDraft(
                name =
                    "Bedtime",
                mode =
                    "downtime",
                timezone =
                    "UTC"
            )
                .toSchedule(
                    initialSchedule =
                        null
                )

        assertEquals(
            "",
            schedule.id
        )
    }

    @Test
    fun `downtime rules serialize only block actions`() {

        val schedule =
            ScheduleDraft(
                name =
                    "Bedtime",
                mode =
                    "downtime",
                timezone =
                    "UTC",
                rules =
                    listOf(
                        ScheduleRuleDraft(
                            startTime =
                                "22:00",
                            endTime =
                                "06:00"
                        )
                    )
            )
                .toSchedule(
                    null
                )

        assertTrue(
            schedule.safeRules
                .all {
                    it.action ==
                        "block"
                }
        )
    }

    @Test
    fun `whitelist rules serialize only allow actions`() {

        val schedule =
            ScheduleDraft(
                name =
                    "Homework",
                mode =
                    "whitelist",
                timezone =
                    "UTC",
                rules =
                    listOf(
                        ScheduleRuleDraft(
                            startTime =
                                "15:00",
                            endTime =
                                "17:00"
                        )
                    )
            )
                .toSchedule(
                    null
                )

        assertTrue(
            schedule.safeRules
                .all {
                    it.action ==
                        "allow"
                }
        )
    }

    @Test
    fun `overnight window is valid and identified as next day`() {

        val rule =
            ScheduleRuleDraft(
                startTime =
                    "22:00",
                endTime =
                    "06:00"
            )

        assertTrue(
            rule.isOvernight
        )

        val validation =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "Bedtime",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                rule
                            )
                    )
                )

        assertTrue(
            validation.valid
        )
    }

    @Test
    fun `same time schedule window is rejected`() {

        val validation =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "Invalid",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                ScheduleRuleDraft(
                                    startTime =
                                        "22:00",
                                    endTime =
                                        "22:00"
                                )
                            )
                    )
                )

        assertFalse(
            validation.valid
        )
    }

    @Test
    fun `calendar range requires both dates`() {

        val validation =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "Holiday",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                ScheduleRuleDraft(
                                    scope =
                                        ScheduleRuleScope.CALENDAR,
                                    startDate =
                                        "2026-12-20",
                                    endDate =
                                        ""
                                )
                            )
                    )
                )

        assertFalse(
            validation.valid
        )
    }

    @Test
    fun `calendar range rejects reversed dates`() {

        val validation =
            ScheduleSemantics
                .validate(
                    ScheduleDraft(
                        name =
                            "Holiday",
                        timezone =
                            "UTC",
                        rules =
                            listOf(
                                ScheduleRuleDraft(
                                    scope =
                                        ScheduleRuleScope.CALENDAR,
                                    startDate =
                                        "2027-01-10",
                                    endDate =
                                        "2027-01-01"
                                )
                            )
                    )
                )

        assertFalse(
            validation.valid
        )
    }

    @Test
    fun `authentication error points user to connection settings`() {

        val presentation =
            ErrorPresentation
                .from(
                    ApiResult.AuthenticationError(
                        code =
                            401,
                        message =
                            "invalid token"
                    )
                )

        assertTrue(
            presentation
                .requiresConnectionSettings
        )

        assertFalse(
            presentation.retryable
        )
    }

    @Test
    fun `network error is retryable`() {

        val presentation =
            ErrorPresentation
                .from(
                    ApiResult.NetworkError(
                        IOException(
                            "connection refused"
                        )
                    )
                )

        assertTrue(
            presentation.retryable
        )

        assertTrue(
            presentation
                .requiresConnectionSettings
        )
    }

    @Test
    fun `serialization failure is not mislabeled as network failure`() {

        val presentation =
            ErrorPresentation
                .from(
                    ApiResult.SerializationError(
                        message =
                            "invalid payload"
                    )
                )

        assertTrue(
            presentation.title
                .contains(
                    "Incompatible",
                    ignoreCase =
                        true
                )
        )

        assertFalse(
            presentation.retryable
        )
    }

    @Test
    fun `diagnostics redact bearer credentials`() {

        val secret =
            "top-secret-token"

        val record =
            ErrorPresentation
                .diagnostic(
                    ApiResult.NetworkError(
                        IOException(
                            "Authorization: Bearer $secret"
                        )
                    ),
                    endpoint =
                        "http://192.168.1.10:8081/api/v1/health?token=$secret"
                )

        assertEquals(
            DiagnosticKind.NETWORK,
            record.kind
        )

        assertFalse(
            record.technicalDetail
                .orEmpty()
                .contains(
                    secret
                )
        )
    }

    @Test
    fun `diagnostic endpoint strips path and query`() {

        assertEquals(
            "https://lias.example.com:8443",
            ErrorPresentation
                .safeEndpoint(
                    "https://lias.example.com:8443/api/v1/health?token=secret"
                )
        )
    }

    @Test
    fun `settings are not configured until persisted URL exists`() {

        val state =
            SettingsUiState(
                serverUrl =
                    "http://candidate:8081",
                savedServerUrl =
                    "",
                isConfigurationLoaded =
                    true
            )

        assertFalse(
            state.isConfigured
        )
    }

    @Test
    fun `saved server marks configuration complete independently of SSE`() {

        val state =
            SettingsUiState(
                serverUrl =
                    "http://lias:8081",
                savedServerUrl =
                    "http://lias:8081",
                isConfigurationLoaded =
                    true
            )

        assertTrue(
            state.isConfigured
        )
    }

    @Test
    fun `connection draft recognizes server replacement`() {

        val state =
            SettingsUiState(
                serverUrl =
                    "http://new-server:8081",
                savedServerUrl =
                    "http://old-server:8081",
                authToken =
                    "token",
                savedAuthToken =
                    "token",
                isConfigurationLoaded =
                    true
            )

        assertTrue(
            state.hasConnectionDraftChanges
        )
    }

    @Test
    fun `home deep link parses`() {

        assertEquals(
            ExternalDestination.Home,
            LiasDeepLinks.parse(
                "liasremote://home"
            )
        )
    }

    @Test
    fun `device deep link preserves pdid`() {

        val destination =
            LiasDeepLinks.parse(
                "liasremote://device/pdid_abc123"
            )

        assertTrue(
            destination is
                ExternalDestination.Device
        )

        assertEquals(
            "pdid_abc123",
            (
                destination as
                    ExternalDestination.Device
                ).pdid
        )
    }

    @Test
    fun `devices slash pdid deep link is also supported`() {

        val destination =
            LiasDeepLinks.parse(
                "liasremote://devices/pdid_42"
            )

        assertEquals(
            ExternalDestination.Device(
                "pdid_42"
            ),
            destination
        )
    }

    @Test
    fun `unknown deep link host is rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "liasremote://delete-everything"
            )
        )
    }

    @Test
    fun `foreign deep link scheme is rejected`() {

        assertNull(
            LiasDeepLinks.parse(
                "https://example.com/device/foo"
            )
        )
    }

    @Test
    fun `deep link cannot inject server or token configuration`() {

        /*
         * The parser has no configuration destination and therefore
         * rejects arbitrary credential-bearing hosts.
         */
        assertNull(
            LiasDeepLinks.parse(
                "liasremote://connect?server=http://evil&token=secret"
            )
        )
    }
}
