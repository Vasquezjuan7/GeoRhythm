package com.example.georhythm.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceTierTest {

    @Test
    fun `cada cadencia cae en el nivel correcto`() {
        assertEquals(PaceTier.STOPPED, PaceTier.from(0))
        assertEquals(PaceTier.STOPPED, PaceTier.from(59))
        assertEquals(PaceTier.STROLL, PaceTier.from(60))
        assertEquals(PaceTier.WALK, PaceTier.from(115))
        assertEquals(PaceTier.BRISK, PaceTier.from(130))
        assertEquals(PaceTier.JOG, PaceTier.from(160))
    }

    @Test
    fun `los niveles estan ordenados por cadencia minima`() {
        val mins = PaceTier.entries.map { it.minSpm }
        assertEquals(mins.sorted(), mins)
    }

    @Test
    fun `todos los niveles tienen busqueda para Spotify`() {
        PaceTier.entries.forEach { assertTrue(it.name, it.spotifyQuery.isNotBlank()) }
    }
}
