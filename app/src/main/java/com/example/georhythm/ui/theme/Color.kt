package com.example.georhythm.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Paleta "Sonic Journey"
val Emerald = Color(0xFF10B981)
val EmeraldDeep = Color(0xFF047857)
val Mint = Color(0xFF22D3EE)
val SpotifyGreen = Color(0xFF1DB954)
val Gold = Color(0xFFF59E0B)
val NavBlue = Color(0xFF1A73E8)
val Violet = Color(0xFF8B5CF6)
val Danger = Color(0xFFEF4444)

/** Colores semánticos para día/noche. */
@Immutable
data class GeoColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val glass: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
    val onAccent: Color = Color(0xFF04140E)
) {
    val accentGradient: Brush get() = Brush.linearGradient(listOf(Emerald, Mint))
    val musicGradient: Brush get() = Brush.linearGradient(listOf(Violet, Mint))
}

val NightColors = GeoColors(
    isDark = true,
    background = Color(0xFF0B1326),
    surface = Color(0xFF151E33),
    surfaceAlt = Color(0xFF1C2742),
    glass = Color(0xFF111A2E).copy(alpha = 0.92f),
    text = Color(0xFFF1F5F9),
    textMuted = Color(0xFF94A3B8),
    border = Color.White.copy(alpha = 0.08f)
)

val DayColors = GeoColors(
    isDark = false,
    background = Color(0xFFF4F6FA),
    surface = Color.White,
    surfaceAlt = Color(0xFFEFF2F7),
    glass = Color.White.copy(alpha = 0.95f),
    text = Color(0xFF0F172A),
    textMuted = Color(0xFF64748B),
    border = Color(0xFFE2E8F0)
)

val LocalGeoColors = staticCompositionLocalOf { NightColors }
