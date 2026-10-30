package com.example.georhythm.data

data class GpxPoint(
    val lat: Double,
    val lon: Double,
    val elevationM: Double? = null,
    val timestampIso: String? = null,
    val cadenceSpm: Int? = null,
    val speedKmh: Float? = null
)

data class GpxWaypoint(
    val lat: Double,
    val lon: Double,
    val name: String,
    val description: String? = null
)

data class GpxTrack(
    val name: String,
    val description: String,
    val creator: String = "GeoRhythm Sonic Journey",
    val waypoints: List<GpxWaypoint> = emptyList(),
    val points: List<GpxPoint> = emptyList()
)
