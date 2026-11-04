package com.example.georhythm.audio

import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCuePriorityTest {

    @Test
    fun `indicaciones de giro tienen mayor prioridad que avisos de cadencia`() {
        val turnCue = AudioCueGenerator.forTurn("Gira a la derecha por la Calle 19")
        val cadenceCue = AudioCueGenerator.forCadenceChange(125)
        assertTrue(turnCue.priority.level > cadenceCue.priority.level)
    }
}
