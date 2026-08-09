package com.lias.remote.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestructiveDeleteContractTest {

    @Test
    fun `saved objects require protected deletion`() {
        assertTrue(requiresProtectedDelete("pol_abc"))
        assertTrue(requiresProtectedDelete("sched_abc"))
    }

    @Test
    fun `unsaved drafts do not require protected deletion`() {
        assertFalse(requiresProtectedDelete(""))
        assertFalse(requiresProtectedDelete("   "))
    }
}
