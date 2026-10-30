package com.example.georhythm.util

object FitnessMath {

    const val DEFAULT_WEIGHT_KG = 70.0

    fun calculateMet(speedKmh: Double): Double = when {
        speedKmh < 1.0 -> 1.0
        speedKmh < 3.2 -> 2.5
        speedKmh < 4.5 -> 3.3
        speedKmh < 5.6 -> 3.8
        speedKmh < 6.4 -> 4.5
        else -> 6.0
    }

    fun caloriesBurned(speedKmh: Double, durationMinutes: Double, weightKg: Double = DEFAULT_WEIGHT_KG): Double {
        if (durationMinutes <= 0.0 || speedKmh <= 0.0) return 0.0
        val met = calculateMet(speedKmh)
        return (met * 3.5 * weightKg / 200.0) * durationMinutes
    }

    fun estimateSteps(cadenceSpm: Int, durationSeconds: Long): Long {
        if (cadenceSpm <= 0 || durationSeconds <= 0) return 0L
        return (cadenceSpm.toDouble() * (durationSeconds / 60.0)).toLong()
    }
}
