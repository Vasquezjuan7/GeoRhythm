package com.example.georhythm.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ColorContrastTest {

    private fun luminance(r: Double, g: Double, b: Double): Double {
        fun f(c: Double) = if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)
    }

    private fun contrastRatio(l1: Double, l2: Double): Double {
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun `contraste entre texto blanco y fondo oscuro supera 4 punto 5 a 1`() {
        val darkBgL = luminance(0.05, 0.07, 0.12)
        val whiteTextL = luminance(1.0, 1.0, 1.0)
        val ratio = contrastRatio(darkBgL, whiteTextL)
        assertTrue(ratio >= 4.5)
    }
}
