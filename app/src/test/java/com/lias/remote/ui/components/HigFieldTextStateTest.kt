// ====================================================================
// File: app/src/test/java/com/lias/remote/ui/components/HigFieldTextStateTest.kt
//
// Purpose:
//   Plan 3.1 text-field polish guard for cursor/selection reconciliation.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HigFieldTextStateTest {

    @Test
    fun reconcilePreservesSelectionInsideNewTextBounds() {
        val current =
            TextFieldValue(
                text = "Kitchen iPad",
                selection =
                    TextRange(
                        7,
                        7
                    )
            )

        val updated =
            reconcileEditorValue(
                current =
                    current,
                externalText =
                    "Kitchen iPad Pro"
            )

        assertEquals(
            "Kitchen iPad Pro",
            updated.text
        )
        assertEquals(
            TextRange(
                7,
                7
            ),
            updated.selection
        )
        assertNull(
            updated.composition
        )
    }

    @Test
    fun reconcileClampsSelectionWhenExternalTextShrinks() {
        val current =
            TextFieldValue(
                text = "Very Long Device Name",
                selection =
                    TextRange(
                        21,
                        21
                    )
            )

        val updated =
            reconcileEditorValue(
                current =
                    current,
                externalText =
                    "Short"
            )

        assertEquals(
            "Short",
            updated.text
        )
        assertEquals(
            TextRange(
                5,
                5
            ),
            updated.selection
        )
        assertNull(
            updated.composition
        )
    }

    @Test
    fun reconcileReturnsSameValueWhenTextMatches() {
        val current =
            TextFieldValue(
                text = "Living Room TV",
                selection =
                    TextRange(
                        4,
                        4
                    )
            )

        val updated =
            reconcileEditorValue(
                current =
                    current,
                externalText =
                    "Living Room TV"
            )

        assertEquals(
            current,
            updated
        )
    }
}
