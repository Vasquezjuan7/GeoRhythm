package com.example.georhythm.network

import com.example.georhythm.data.RouteGeometry

data class RouteCacheKey(
    val startLatRound: Long,
    val startLonRound: Long,
    val endLatRound: Long,
    val endLonRound: Long
) {
    companion object {
        fun fromCoords(startLat: Double, startLon: Double, endLat: Double, endLon: Double): RouteCacheKey {
            return RouteCacheKey(
                (startLat * 1000).toLong(),
                (startLon * 1000).toLong(),
                (endLat * 1000).toLong(),
                (endLon * 1000).toLong()
            )
        }
    }
}

data class CachedRoute(
    val geometry: RouteGeometry,
    val timestampMs: Long = System.currentTimeMillis()
)
