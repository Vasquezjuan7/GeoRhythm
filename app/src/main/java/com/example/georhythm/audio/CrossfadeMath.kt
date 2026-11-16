package com.example.georhythm.audio

import kotlin.math.cos
import kotlin.math.sin

object CrossfadeMath {

    fun calculateVolumes(progress: Float, curve: CrossfadeCurve = CrossfadeCurve.EQUAL_POWER): Pair<Float, Float> {
        val p = progress.coerceIn(0f, 1f)
        return when (curve) {
            CrossfadeCurve.LINEAR -> (1f - p) to p
            CrossfadeCurve.EQUAL_POWER -> {
                val rad = (p * Math.PI / 2.0).toFloat()
                cos(rad) to sin(rad)
            }
            CrossfadeCurve.SMOOTH_STEP -> {
                val s = p * p * (3f - 2f * p)
                (1f - s) to s
            }
        }
    }
}
