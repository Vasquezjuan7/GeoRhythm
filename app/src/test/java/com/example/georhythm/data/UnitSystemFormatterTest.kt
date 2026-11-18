package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UnitSystemFormatterTest {

    private fun formatDistance(meters: Double, system: UnitSystem): String {
        return when (system) {
            UnitSystem.METRIC -> {
                if (meters < 1000) String.format(Locale.US, "%.0f m", meters)
                else String.format(Locale.US, "%.1f km", meters / 1000.0)
            }
            UnitSystem.IMPERIAL -> {
                val miles = meters * 0.000621371
                if (miles < 0.1) {
                    val feet = meters * 3.28084
                    String.format(Locale.US, "%.0f ft", feet)
                } else {
                    String.format(Locale.US, "%.2f mi", miles)
                }
            }
        }
    }

    @Test
    fun `formatea distancias en sistema metrico e imperial`() {
        assertEquals("450 m", formatDistance(450.0, UnitSystem.METRIC))
        assertEquals("1.5 km", formatDistance(1500.0, UnitSystem.METRIC))
        assertEquals("1476 ft", formatDistance(450.0, UnitSystem.IMPERIAL))
        assertEquals("0.93 mi", formatDistance(1500.0, UnitSystem.IMPERIAL))
    }
}
