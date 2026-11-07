package com.example.georhythm.data

enum class WalkerProfile(
    val averageSpeedKmh: Float,
    val targetCadenceSpm: Int
) {
    LEISURE_STROLL(3.2f, 95),
    ACTIVE_WALK(4.8f, 118),
    POWER_WALK(6.2f, 135)
}
