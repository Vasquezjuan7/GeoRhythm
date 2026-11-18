package com.example.georhythm.ui.components

enum class HapticPattern {
    LIGHT_TICK,
    MEDIUM_TAP,
    DOUBLE_PULSE,
    SUCCESS_CONFIRM
}

class HapticController {
    fun patternForTurn(): HapticPattern = HapticPattern.DOUBLE_PULSE
    fun patternForCadenceMatch(): HapticPattern = HapticPattern.SUCCESS_CONFIRM
    fun patternForTick(): HapticPattern = HapticPattern.LIGHT_TICK
}
