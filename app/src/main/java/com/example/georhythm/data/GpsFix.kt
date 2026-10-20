package com.example.georhythm.data

import android.location.Location

/**
 * Lectura GPS independiente de Android, para que la lógica de caminata se pueda probar en la JVM.
 */
data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    /** Precisión horizontal en metros; null si se desconoce. */
    val accuracyM: Float? = null,
    /** Velocidad informada por el GPS en m/s; null si el GPS no la entrega. */
    val speedMps: Float? = null,
    val bearingDeg: Float? = null,
    val timeMs: Long = System.currentTimeMillis()
)

fun Location.toGpsFix() = GpsFix(
    latitude = latitude,
    longitude = longitude,
    accuracyM = if (hasAccuracy()) accuracy else null,
    speedMps = if (hasSpeed()) speed else null,
    bearingDeg = if (hasBearing()) bearing else null,
    timeMs = time
)
