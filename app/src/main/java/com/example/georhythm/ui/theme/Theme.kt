package com.example.georhythm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun GeoRhythmTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val geo = if (darkTheme) NightColors else DayColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = Emerald, onPrimary = geo.onAccent, secondary = Mint, tertiary = Gold,
            background = geo.background, surface = geo.surface, onBackground = geo.text, onSurface = geo.text,
            surfaceVariant = geo.surfaceAlt, onSurfaceVariant = geo.textMuted, outline = geo.border, error = Danger
        )
    } else {
        lightColorScheme(
            primary = EmeraldDeep, onPrimary = androidx.compose.ui.graphics.Color.White, secondary = NavBlue,
            tertiary = Gold, background = geo.background, surface = geo.surface, onBackground = geo.text,
            onSurface = geo.text, surfaceVariant = geo.surfaceAlt, onSurfaceVariant = geo.textMuted,
            outline = geo.border, error = Danger
        )
    }
    CompositionLocalProvider(LocalGeoColors provides geo) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
