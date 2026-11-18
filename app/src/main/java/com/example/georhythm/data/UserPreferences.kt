package com.example.georhythm.data

enum class UnitSystem { METRIC, IMPERIAL }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val voiceGuidanceEnabled: Boolean = true,
    val audioDuckingEnabled: Boolean = true,
    val cadenceSensitivity: Float = 1.0f,
    val autoPauseDelaySeconds: Int = 10,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
