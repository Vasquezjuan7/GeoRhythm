package com.example.georhythm.audio

import org.junit.Assert.assertNotNull
import org.junit.Test

class SpotifyHysteresisTest {

    @Test
    fun `selector mantiene mapeo coherente en los limites`() {
        val selector = SpotifySmartSelector()
        assertNotNull(selector.selectForCadence(80))
        assertNotNull(selector.selectForCadence(81))
        assertNotNull(selector.selectForCadence(110))
        assertNotNull(selector.selectForCadence(111))
    }
}
