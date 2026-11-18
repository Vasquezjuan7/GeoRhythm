package com.example.georhythm.data

import org.junit.Assert.assertNotEquals
import org.junit.Test

class WalkSimulatorNoiseTest {

    @Test
    fun `ruido simulado introduce variaciones leves pero realistas`() {
        val ptsClean = WalkSimulator.generatePoints(1.0, -77.0, 2.0, -76.0, steps = 5, addJitter = false)
        val ptsNoisy = WalkSimulator.generatePoints(1.0, -77.0, 2.0, -76.0, steps = 5, addJitter = true)
        assertNotEquals(ptsClean[1].latitude, ptsNoisy[1].latitude)
    }
}
