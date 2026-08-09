package com.lias.remote.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticThemeContractTest {

    @Test
    fun `dark semantic labels remain visible`() {
        assertTrue(SystemLabelDark.alpha > 0.95f)
        assertTrue(SystemSecondaryLabelDark.alpha > 0.45f)
        assertTrue(SystemTertiaryLabelDark.alpha > 0.45f)
    }

    @Test
    fun `primary labels contrast their backgrounds`() {
        assertTrue(SystemLabelLight != SystemBackgroundLight)
        assertTrue(SystemLabelDark != SystemBackgroundDark)
    }
}
