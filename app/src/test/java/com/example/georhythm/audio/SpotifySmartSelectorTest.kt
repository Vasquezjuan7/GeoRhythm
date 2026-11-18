package com.example.georhythm.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotifySmartSelectorTest {

    @Test
    fun `selecciona playlist adecuada segun el rango de cadencia`() {
        val selector = SpotifySmartSelector()
        assertEquals("Paseo Calmo & Chill", selector.selectForCadence(65).playlistName)
        assertEquals("Caminata Urbana", selector.selectForCadence(95).playlistName)
        assertEquals("Paso Firme San Juan", selector.selectForCadence(120).playlistName)
        assertEquals("Trote Energético", selector.selectForCadence(145).playlistName)
    }
}
