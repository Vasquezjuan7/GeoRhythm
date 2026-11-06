package com.example.georhythm.ui.components

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.georhythm.ui.theme.LocalGeoColors
import org.maplibre.android.geometry.LatLng
import java.util.Locale
import kotlin.math.roundToInt

// Plaza de Nariño (verificada en OpenStreetMap)
val PASTO_CENTER = LatLng(1.214611, -77.278280)

/** Descarta lecturas inválidas (0,0) o fuera de Nariño. */
fun isLocationInPastoRegion(location: Location?): Boolean {
    if (location == null) return false
    return location.latitude in 0.5..1.9 && location.longitude in -78.5..-76.5
}

/** Distancia geodésica real (km) desde el GPS del usuario; null si no hay ubicación válida. */
fun calculateDistanceKm(userLoc: Location?, target: LatLng): Float? {
    if (!isLocationInPastoRegion(userLoc)) return null
    val out = FloatArray(1)
    Location.distanceBetween(userLoc!!.latitude, userLoc.longitude, target.latitude, target.longitude, out)
    return out[0] / 1000f
}

fun formatDistanceText(km: Float): String =
    if (km < 1f) "${(km * 1000).roundToInt()} m" else String.format(Locale.US, "%.1f km", km)

/** Tiempo a pie a 4.8 km/h. */
fun formatWalkingTimeText(km: Float): String {
    val mins = (km / 4.8f * 60f).roundToInt().coerceAtLeast(1)
    return if (mins < 60) "$mins min" else "${mins / 60} h ${mins % 60} min"
}

/** Más allá de esto no se ofrece navegación a pie. */
const val MAX_WALKING_KM = 12f

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    elevation: Dp = 10.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalGeoColors.current
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(shape)
            .background(c.glass)
            .border(1.dp, c.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content
    )
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (filled) Color.White else color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = if (filled) Color.White else color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    val c = LocalGeoColors.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = c.text)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        trailing?.invoke()
    }
}

@Composable
fun StatTile(label: String, value: String, accent: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    val c = LocalGeoColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(value, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = c.textMuted, fontSize = 11.sp)
    }
}

fun parseHexColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
