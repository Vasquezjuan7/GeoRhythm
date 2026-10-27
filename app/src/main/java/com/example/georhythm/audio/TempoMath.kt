package com.example.georhythm.audio

import com.example.georhythm.data.AudiusTrack
import kotlin.math.abs

/**
 * Matemática de "Ritmo al paso": relaciona el BPM de una canción con la cadencia de pasos.
 * Sin dependencias de Android para poder probarla en la JVM.
 */
object TempoMath {
    /** Rango de velocidad de reproducción que no se nota (el tono no cambia). */
    const val MIN_RATE = 0.92f
    const val MAX_RATE = 1.10f
    /** Desajuste relativo a partir del cual conviene cambiar de canción. */
    const val MISMATCH_THRESHOLD = 0.14f

    /** BPM de la canción más cercano a la cadencia, permitiendo medio y doble tiempo. */
    fun effectiveBpm(bpm: Int, cadence: Int): Int =
        listOf(bpm, bpm * 2, bpm / 2).minBy { abs(it - cadence) }

    /** Velocidad de reproducción para que la canción vaya al paso, limitada al rango imperceptible. */
    fun playbackRate(bpm: Int, cadence: Int): Float =
        (cadence.toFloat() / effectiveBpm(bpm, cadence)).coerceIn(MIN_RATE, MAX_RATE)

    /** true si ni ajustando el tempo la canción alcanza la cadencia. */
    fun isMismatch(bpm: Int, cadence: Int): Boolean {
        val eff = effectiveBpm(bpm, cadence)
        return abs(eff - cadence).toFloat() / eff > MISMATCH_THRESHOLD
    }

    /** Canciones ordenadas de mejor a peor ajuste a la cadencia (sólo las que tienen BPM). */
    fun rankByFit(tracks: List<AudiusTrack>, cadence: Int): List<AudiusTrack> =
        tracks.filter { it.bpm != null }
            .sortedBy { abs(effectiveBpm(it.bpm!!, cadence) - cadence) }
}
