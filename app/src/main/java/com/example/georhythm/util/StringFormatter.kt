package com.example.georhythm.util

import java.util.Locale

object StringFormatter {

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    fun formatPace(minutesPerKm: Double): String {
        val mins = minutesPerKm.toInt()
        val secs = ((minutesPerKm - mins) * 60).toInt()
        return String.format(Locale.US, "%d'%02d\"/km", mins, secs)
    }
}
