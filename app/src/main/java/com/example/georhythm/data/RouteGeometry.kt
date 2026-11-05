package com.example.georhythm.data

data class RouteGeometry(
    val points: List<Pair<Double, Double>>,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double
)
