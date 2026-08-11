package com.lias.remote.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HigFieldStateTest {

    @Test
    fun `external text update preserves an in-range cursor`() {
        val current =
            TextFieldValue(
                text = "http://lias.local:8081",
                selection = TextRange(7)
            )

        val updated =
            reconcileEditorValue(
                current,
                "https://lias.local:8081"
            )

        assertEquals("https://lias.local:8081", updated.text)
        assertEquals(TextRange(7), updated.selection)
    }

    @Test
    fun `shorter external text clamps selection instead of jumping`() {
        val current =
            TextFieldValue(
                text = "A long device name",
                selection = TextRange(18, 10)
            )

        val updated = reconcileEditorValue(current, "Router")

        assertEquals(TextRange(6, 6), updated.selection)
        assertNull(updated.composition)
    }

    @Test
    fun `unchanged text retains the complete editor value`() {
        val current =
            TextFieldValue(
                text = "Kids",
                selection = TextRange(2)
            )

        assertSame(current, reconcileEditorValue(current, "Kids"))
    }
}
