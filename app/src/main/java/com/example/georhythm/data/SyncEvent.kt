package com.example.georhythm.data

enum class SyncEventType { WALK_SESSION, POI_DISCOVERY, FAVORITE_TOGGLE }

data class SyncEvent(
    val id: String,
    val type: SyncEventType,
    val payloadJson: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)
