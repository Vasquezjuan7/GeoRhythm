package com.example.georhythm.data

import com.example.georhythm.util.GeoMath

/**
 * Suaviza la velocidad del GPS (media móvil exponencial) y descarta valores imposibles para
 * una caminata. Si el GPS no entrega velocidad, se calcula con distancia / tiempo.
 */
class SpeedFilter(
    private val alpha: Float = 0.35f,
    private val maxMps: Float = 8f,
    private val stillThresholdMps: Float = 0.35f
) {
    private var last: GpsFix? = null
    private var smoothed = 0f

    /** Devuelve la velocidad suavizada en km/h (0 si se considera quieto). */
    fun update(fix: GpsFix): Float {
        val prev = last
        val raw = when {
            fix.speedMps != null && fix.speedMps > 0f -> fix.speedMps
            prev != null -> {
                val dt = (fix.timeMs - prev.timeMs) / 1000f
                if (dt in 0.5f..30f) {
                    (GeoMath.distanceMeters(prev.latitude, prev.longitude, fix.latitude, fix.longitude) / dt).toFloat()
                } else 0f
            }
            else -> 0f
        }.coerceIn(0f, maxMps)
        smoothed = if (prev == null) raw else smoothed * (1 - alpha) + raw * alpha
        last = fix
        return if (smoothed < stillThresholdMps) 0f else smoothed * 3.6f
    }

    fun reset() {
        last = null
        smoothed = 0f
    }
}
