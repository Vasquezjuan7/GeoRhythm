package com.example.georhythm.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class CrossfadePauseTest {

    @Test
    fun `curva lineal reparte proporciones exactamente complementarias`() {
        val (vOut, vIn) = CrossfadeMath.calculateVolumes(0.3f, CrossfadeCurve.LINEAR)
        assertEquals(0.7f, vOut, 1e-4f)
        assertEquals(0.3f, vIn, 1e-4f)
    }
}
