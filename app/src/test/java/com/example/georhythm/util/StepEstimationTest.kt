package com.example.georhythm.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StepEstimationTest {

    @Test
    fun `pasos estimados a cadencia fija coinciden con la duracion`() {
        assertEquals(1200L, FitnessMath.estimateSteps(120, 600))
        assertEquals(0L, FitnessMath.estimateSteps(0, 600))
        assertEquals(0L, FitnessMath.estimateSteps(120, 0))
    }
}
