package com.example.georhythm.data

import com.example.georhythm.util.FitnessMath

data class WalkStats(
    val distanceMeters: Double,
    val durationSeconds: Long,
    val averageSpeedKmh: Double,
    val averageCadenceSpm: Int,
    val caloriesKcal: Double = FitnessMath.caloriesBurned(averageSpeedKmh, durationSeconds / 60.0),
    val totalSteps: Long = FitnessMath.estimateSteps(averageCadenceSpm, durationSeconds)
)
