package com.lias.remote.core.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyDraftPersistenceContractTest {

    @Test
    fun `new tag policy keeps server owned id and exact target`() {
        val policy = PolicyDraft(
            name = "Kids Block",
            type = "tag",
            targetId = "kids",
            action = "block",
            enabled = true
        ).toPolicy(null)

        assertEquals("", policy.id)
        assertEquals("tag", policy.type)
        assertEquals("kids", policy.targetID)
        assertEquals("block", policy.action)
        assertTrue(policy.resolveScheduleIDs().isEmpty())
    }

    @Test
    fun `new device policy keeps pdid schedule and priority`() {
        val policy = PolicyDraft(
            name = "Tablet Bedtime",
            type = "device",
            targetId = "pdid_tablet_123",
            action = "schedule",
            scheduleIds = setOf("sched_bedtime"),
            priorityText = "90",
            enabled = true
        ).toPolicy(null)

        assertEquals("", policy.id)
        assertEquals("device", policy.type)
        assertEquals("pdid_tablet_123", policy.targetID)
        assertEquals(90, policy.priority)
        assertEquals(listOf("sched_bedtime"), policy.resolveScheduleIDs())
    }
}
