package com.example.georhythm.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cálculos geográficos puros (sin dependencias de Android), para poder probarlos en la JVM.
 */
object GeoMath {
    /** Radio medio de la Tierra en metros (WGS-84). */
    const val EARTH_RADIUS_M = 6_371_008.8

    /** Distancia geodésica en metros entre dos coordenadas (fórmula de Haversine). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1 - a))
    }
}
