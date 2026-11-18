package com.example.georhythm.data

import kotlin.math.sin

object WalkSimulator {

    fun generatePoints(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        steps: Int,
        profile: WalkerProfile = WalkerProfile.ACTIVE_WALK,
        addJitter: Boolean = false
    ): List<GpsFix> {
        val list = mutableListOf<GpsFix>()
        val baseTime = 10_000L
        for (i in 0 until steps) {
            val frac = i.toDouble() / (steps - 1)
            var lat = startLat + (endLat - startLat) * frac
            var lon = startLon + (endLon - startLon) * frac
            if (addJitter) {
                lat += sin(i.toDouble()) * 0.00002
                lon += sin((i + 1).toDouble()) * 0.00002
            }
            val time = baseTime + i * 2000L
            list.add(GpsFix(lat, lon, accuracyM = 5.0f, speedMps = profile.averageSpeedKmh / 3.6f, timeMs = time))
        }
        return list
    }
}
