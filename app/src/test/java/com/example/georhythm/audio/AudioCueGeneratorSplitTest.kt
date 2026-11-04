package com.example.georhythm.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCueGeneratorSplitTest {

    @Test
    fun `genera aviso al completar cada kilometro con minutos y segundos`() {
        val cue = AudioCueGenerator.forSplitKilometer(2, 6.5)
        assertEquals(CueType.SPLIT_KILOMETER, cue.type)
        assertEquals(CuePriority.HIGH, cue.priority)
        assertTrue(cue.message.contains("Kilómetro 2"))
        assertTrue(cue.message.contains("6 minutos 30 segundos"))
    }
}
