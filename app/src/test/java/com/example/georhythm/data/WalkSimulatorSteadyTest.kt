package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkSimulatorSteadyTest {

    @Test
    fun `genera puntos distribuidos uniformemente entre inicio y fin`() {
        val pts = WalkSimulator.generatePoints(1.0, -77.0, 2.0, -76.0, steps = 5, addJitter = false)
        assertEquals(5, pts.size)
        assertEquals(1.0, pts.first().latitude, 1e-6)
        assertEquals(2.0, pts.last().latitude, 1e-6)
    }
}
