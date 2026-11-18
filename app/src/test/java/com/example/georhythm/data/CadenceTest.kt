package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CadenceTest {

    @Test
    fun `quieto o casi quieto no tiene cadencia`() {
        assertEquals(0, WalkTracker.cadenceFor(0f))
        assertEquals(0, WalkTracker.cadenceFor(0.9f))
    }

    @Test
    fun `caminata tipica esta entre 95 y 135 pasos por minuto`() {
        assertEquals(97, WalkTracker.cadenceFor(3f))
        assertEquals(119, WalkTracker.cadenceFor(5f))
        assertEquals(130, WalkTracker.cadenceFor(6f))
    }

    @Test
    fun `la cadencia crece con la velocidad`() {
        var prev = 0
        for (kmh in listOf(2f, 4f, 6f, 8f, 10f)) {
            val c = WalkTracker.cadenceFor(kmh)
            assertTrue("$kmh km/h → $c", c > prev)
            prev = c
        }
    }

    @Test
    fun `la cadencia tiene un tope de 185`() {
        assertEquals(185, WalkTracker.cadenceFor(30f))
    }
}
