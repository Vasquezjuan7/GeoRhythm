package com.example.georhythm.ui

import com.example.georhythm.ui.components.HapticController
import com.example.georhythm.ui.components.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Test

class ShapeTokensTest {

    @Test
    fun `patron haptico de giro emite doble pulso`() {
        val controller = HapticController()
        assertEquals(HapticPattern.DOUBLE_PULSE, controller.patternForTurn())
        assertEquals(HapticPattern.SUCCESS_CONFIRM, controller.patternForCadenceMatch())
    }
}
