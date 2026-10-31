package com.example.georhythm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessMathTest {

    @Test
    fun `METs aumentan con la velocidad de caminata`() {
        assertEquals(2.5, FitnessMath.calculateMet(2.8), 1e-4)
        assertEquals(3.3, FitnessMath.calculateMet(4.0), 1e-4)
        assertEquals(3.8, FitnessMath.calculateMet(5.0), 1e-4)
        assertEquals(4.5, FitnessMath.calculateMet(6.0), 1e-4)
    }

    @Test
    fun `calorias quemadas para 70kg en 30 minutos a 4kmh son realistas`() {
        val cal = FitnessMath.caloriesBurned(speedKmh = 4.0, durationMinutes = 30.0, weightKg = 70.0)
        assertTrue(cal in 110.0..130.0)
    }
}
