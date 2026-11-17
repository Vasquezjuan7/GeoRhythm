package com.example.georhythm.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class CrossfadeMathTest {

    @Test
    fun `inicio y fin de transicion tienen valores extremos 1 a 0 y 0 a 1`() {
        val (vOut0, vIn0) = CrossfadeMath.calculateVolumes(0f)
        assertEquals(1f, vOut0, 1e-4f)
        assertEquals(0f, vIn0, 1e-4f)

        val (vOut1, vIn1) = CrossfadeMath.calculateVolumes(1f)
        assertEquals(0f, vOut1, 1e-4f)
        assertEquals(1f, vIn1, 1e-4f)
    }

    @Test
    fun `curva de igual potencia conserva la energia sonora a medio camino`() {
        val (vOut, vIn) = CrossfadeMath.calculateVolumes(0.5f, CrossfadeCurve.EQUAL_POWER)
        val power = vOut * vOut + vIn * vIn
        assertEquals(1f, power, 1e-3f)
    }
}
