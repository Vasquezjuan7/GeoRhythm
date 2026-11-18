package com.example.georhythm.audio

import com.example.georhythm.data.AudiusTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoMathTest {

    private fun t(id: String, bpm: Int?) = AudiusTrack(id, id, "x", 180, null, "", bpm)

    @Test
    fun `el BPM efectivo admite medio y doble tiempo`() {
        assertEquals(120, TempoMath.effectiveBpm(120, 118))
        assertEquals(140, TempoMath.effectiveBpm(70, 135))  // doble tiempo
        assertEquals(87, TempoMath.effectiveBpm(174, 90))   // medio tiempo (drum and bass caminando)
    }

    @Test
    fun `cancion al mismo BPM que la cadencia suena a velocidad normal`() {
        assertEquals(1f, TempoMath.playbackRate(120, 120), 1e-6f)
    }

    @Test
    fun `acelera o frena un poco para igualar el paso`() {
        assertEquals(125f / 120f, TempoMath.playbackRate(120, 125), 1e-4f)
        assertEquals(112f / 120f, TempoMath.playbackRate(120, 112), 1e-4f)
    }

    @Test
    fun `la velocidad nunca sale del rango imperceptible`() {
        assertEquals(TempoMath.MAX_RATE, TempoMath.playbackRate(100, 125), 1e-6f)
        assertEquals(TempoMath.MIN_RATE, TempoMath.playbackRate(130, 100), 1e-6f)
    }

    @Test
    fun `detecta cuando la cancion no alcanza el paso`() {
        assertFalse(TempoMath.isMismatch(120, 128))
        assertTrue(TempoMath.isMismatch(100, 130))
    }

    @Test
    fun `ordena las canciones por ajuste e ignora las que no tienen BPM`() {
        val ranked = TempoMath.rankByFit(listOf(t("lenta", 90), t("sin", null), t("justa", 119), t("rapida", 140)), 120)
        assertEquals(listOf("justa", "rapida", "lenta"), ranked.map { it.id })
    }
}
