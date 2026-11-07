package com.example.georhythm.data

import com.example.georhythm.util.ElevationMath

data class ElevationProfile(
    val elevationsMeters: List<Double>,
    val minElevation: Double = elevationsMeters.minOrNull() ?: 2527.0,
    val maxElevation: Double = elevationsMeters.maxOrNull() ?: 2527.0,
    val totalGainMeters: Double = ElevationMath.calculateGain(elevationsMeters)
)
