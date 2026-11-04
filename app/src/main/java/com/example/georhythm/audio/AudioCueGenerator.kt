package com.example.georhythm.audio

import java.util.Locale

object AudioCueGenerator {

    fun forSplitKilometer(kilometer: Int, paceMinutesPerKm: Double): AudioCue {
        val mins = paceMinutesPerKm.toInt()
        val secs = ((paceMinutesPerKm - mins) * 60).toInt()
        val text = String.format(Locale.US, "Kilómetro %d completado a ritmo de %d minutos %d segundos por kilómetro.", kilometer, mins, secs)
        return AudioCue(CueType.SPLIT_KILOMETER, text, CuePriority.HIGH)
    }

    fun forWaypointArrival(name: String): AudioCue {
        return AudioCue(CueType.WAYPOINT_ARRIVAL, "Has llegado a $name.", CuePriority.HIGH)
    }

    fun forCadenceChange(newSpm: Int): AudioCue {
        return AudioCue(CueType.CADENCE_UPDATE, "Cadencia ajustada a $newSpm pasos por minuto.", CuePriority.LOW)
    }

    fun forTurn(instruction: String): AudioCue {
        return AudioCue(CueType.NAVIGATION_TURN, instruction, CuePriority.CRITICAL)
    }
}
