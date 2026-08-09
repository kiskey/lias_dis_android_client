// ====================================================================
// File: app/src/test/java/com/lias/remote/core/policy/PolicySemanticsTest.kt
// Version: 17.0.0
//
// Purpose:
//   Regression tests for Policy Wizard safety semantics.
// ====================================================================

package com.lias.remote.core.policy

import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.models.ScheduleRule
import com.lias.remote.core.models.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicySemanticsTest {

    private val tags =
        listOf(
            Tag(
                id = "infrastructure",
                name = "Infrastructure",
                color = "#8e8e93",
                precedence = 100,
                builtin = true
            ),
            Tag(
                id = "kids",
                name = "Kids Devices",
                color = "#ff9500",
                precedence = 80,
                builtin = true
            )
        )

    private val regularDevice =
        Device(
            pdid = "pdid_child",
            hostname = "tablet",
            tags = listOf(
                "kids"
            )
        )

    private val infrastructureDevice =
        Device(
            pdid = "pdid_router",
            hostname = "router",
            tags = listOf(
                "infrastructure"
            )
        )

    @Test
    fun `infrastructure tag is excluded from target list`() {

        val result =
            PolicySemantics
                .availableTags(
                    tags
                )

        assertTrue(
            result.any {
                it.id ==
                    "kids"
            }
        )

        assertFalse(
            result.any {
                it.id ==
                    "infrastructure"
            }
        )
    }

    @Test
    fun `infrastructure device is excluded from target list`() {

        val result =
            PolicySemantics
                .availableDevices(
                    listOf(
                        regularDevice,
                        infrastructureDevice
                    )
                )

        assertEquals(
            listOf(
                "pdid_child"
            ),
            result.map {
                it.pdid
            }
        )
    }

    @Test
    fun `new global policy is rejected`() {

        val result =
            PolicySemantics
                .validateDraft(
                    draft =
                        PolicyDraft(
                            name = "Another Global",
                            type = "global",
                            action = "block"
                        ),
                    initialPolicy =
                        null,
                    tags =
                        tags,
                    devices =
                        listOf(
                            regularDevice
                        ),
                    schedules =
                        emptyList()
                )

        assertFalse(
            result.valid
        )
    }

    @Test
    fun `existing global default may be edited`() {

        val global =
            Policy(
                id = "global_default",
                name = "Global Access Switch",
                type = "global",
                targetID = "",
                action = "schedule",
                priority = 0,
                enabled = true
            )

        val result =
            PolicySemantics
                .validateDraft(
                    draft =
                        PolicyDraft.fromPolicy(
                            global
                        ),
                    initialPolicy =
                        global,
                    tags =
                        tags,
                    devices =
                        listOf(
                            regularDevice
                        ),
                    schedules =
                        emptyList()
                )

        assertTrue(
            result.valid
        )
    }

    @Test
    fun `schedule rule with no schedules is valid default open`() {

        val result =
            PolicySemantics
                .validateDraft(
                    draft =
                        PolicyDraft(
                            name = "Kids Schedule",
                            type = "tag",
                            targetId = "kids",
                            action = "schedule",
                            scheduleIds =
                                emptySet()
                        ),
                    initialPolicy =
                        null,
                    tags =
                        tags,
                    devices =
                        listOf(
                            regularDevice
                        ),
                    schedules =
                        emptyList()
                )

        assertTrue(
            result.valid
        )
    }

    @Test
    fun `missing schedule reference is rejected`() {

        val result =
            PolicySemantics
                .validateDraft(
                    draft =
                        PolicyDraft(
                            name = "Kids Schedule",
                            type = "tag",
                            targetId = "kids",
                            action = "schedule",
                            scheduleIds =
                                setOf(
                                    "deleted_schedule"
                                )
                        ),
                    initialPolicy =
                        null,
                    tags =
                        tags,
                    devices =
                        listOf(
                            regularDevice
                        ),
                    schedules =
                        emptyList()
                )

        assertFalse(
            result.valid
        )
    }

    @Test
    fun `mixed timezone bundle is detected`() {

        val schedules =
            listOf(
                Schedule(
                    id = "a",
                    name = "Bedtime",
                    mode = "downtime",
                    timezone =
                        "America/Los_Angeles",
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
                                    "23:00",
                                action =
                                    "block"
                            )
                        )
                ),
                Schedule(
                    id = "b",
                    name = "Morning",
                    mode = "whitelist",
                    timezone =
                        "UTC",
                    rules =
                        listOf(
                            ScheduleRule(
                                days =
                                    listOf(
                                        "tue"
                                    ),
                                startTime =
                                    "06:00",
                                endTime =
                                    "07:00",
                                action =
                                    "allow"
                            )
                        )
                )
            )

        assertTrue(
            PolicySemantics
                .hasMixedTimezones(
                    setOf(
                        "a",
                        "b"
                    ),
                    schedules
                )
        )
    }

    @Test
    fun `duplicate device target reports shadow warning`() {

        val existing =
            Policy(
                id = "pol_old",
                name = "Existing Tablet Rule",
                type = "device",
                targetID =
                    "pdid_child",
                action =
                    "block",
                priority =
                    80,
                enabled =
                    true
            )

        val warning =
            PolicySemantics
                .shadowWarning(
                    draft =
                        PolicyDraft(
                            name =
                                "Second Tablet Rule",
                            type =
                                "device",
                            targetId =
                                "pdid_child",
                            action =
                                "allow"
                        ),
                    initialPolicy =
                        null,
                    policies =
                        listOf(
                            existing
                        )
                )

        assertNotNull(
            warning
        )

        assertEquals(
            existing.id,
            warning
                ?.existingPolicy
                ?.id
        )
    }

    @Test
    fun `editing same policy does not shadow itself`() {

        val existing =
            Policy(
                id = "pol_old",
                name = "Existing Tablet Rule",
                type = "device",
                targetID =
                    "pdid_child",
                action =
                    "block",
                priority =
                    80,
                enabled =
                    true
            )

        val warning =
            PolicySemantics
                .shadowWarning(
                    draft =
                        PolicyDraft
                            .fromPolicy(
                                existing
                            ),
                    initialPolicy =
                        existing,
                    policies =
                        listOf(
                            existing
                        )
                )

        assertNull(
            warning
        )
    }

    @Test
    fun `new policy keeps id blank for server generation`() {

        val policy =
            PolicyDraft(
                name =
                    "Kids Rule",
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
    fun `legacy schedule id is normalized into schedule ids`() {

        val old =
            Policy(
                id =
                    "pol_old",
                name =
                    "Old Rule",
                type =
                    "tag",
                targetID =
                    "kids",
                action =
                    "schedule",
                scheduleIDs =
                    emptyList(),
                scheduleID =
                    "sched_old",
                priority =
                    50,
                enabled =
                    true
            )

        val draft =
            PolicyDraft
                .fromPolicy(
                    old
                )

        assertEquals(
            setOf(
                "sched_old"
            ),
            draft.scheduleIds
        )

        val roundTrip =
            draft.toPolicy(
                old
            )

        assertEquals(
            listOf(
                "sched_old"
            ),
            roundTrip.scheduleIDs
        )

        assertNull(
            roundTrip.scheduleID
        )
    }
}
