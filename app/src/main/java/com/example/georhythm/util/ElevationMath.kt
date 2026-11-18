package com.example.georhythm.util

object ElevationMath {

    fun calculateGain(elevations: List<Double>, thresholdMeters: Double = 1.0): Double {
        if (elevations.size < 2) return 0.0
        var gain = 0.0
        for (i in 1 until elevations.size) {
            val delta = elevations[i] - elevations[i - 1]
            if (delta > thresholdMeters) {
                gain += delta
            }
        }
        return gain
    }

    fun gradePercentage(verticalMeters: Double, horizontalMeters: Double): Double {
        if (horizontalMeters <= 0.0) return 0.0
        return (verticalMeters / horizontalMeters) * 100.0
    }
}
