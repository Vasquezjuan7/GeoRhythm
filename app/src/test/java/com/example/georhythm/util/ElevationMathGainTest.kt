package com.example.georhythm.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationMathGainTest {

    @Test
    fun `ganancia de elevacion suma ascensos y filtra ruido menor al umbral`() {
        val samples = listOf(2520.0, 2520.4, 2525.0, 2523.0, 2530.0)
        assertEquals(11.6, ElevationMath.calculateGain(samples, 1.0), 1e-4)
    }
}
