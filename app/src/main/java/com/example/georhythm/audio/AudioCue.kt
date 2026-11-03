package com.example.georhythm.audio

enum class CuePriority(val level: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4)
}

enum class CueType {
    NAVIGATION_TURN,
    WAYPOINT_ARRIVAL,
    CADENCE_UPDATE,
    SPLIT_KILOMETER,
    ROUTE_OFF_TRACK
}

data class AudioCue(
    val type: CueType,
    val message: String,
    val priority: CuePriority = CuePriority.MEDIUM,
    val timestampMs: Long = System.currentTimeMillis()
)
