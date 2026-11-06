package com.example.georhythm.ui.components

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.georhythm.data.NavigationRouteData
import com.example.georhythm.data.RouteStep
import com.example.georhythm.data.WalkTracker
import com.example.georhythm.ui.theme.LocalGeoColors
import com.example.georhythm.ui.theme.Mint
import com.example.georhythm.ui.theme.NavBlue
import org.maplibre.android.geometry.LatLng
import kotlin.math.roundToInt

@Composable
fun NavigationHUD(
    route: NavigationRouteData,
    nextStep: RouteStep?,
    remainingMeters: Double,
    nextStopIndex: Int,
    currentLocation: Location?,
    speedKmh: Float,
    cadenceSpm: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalGeoColors.current
    Column(modifier.padding(horizontal = 12.dp)) {
        // Tarjeta azul de la próxima maniobra
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(NavBlue)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(maneuverIcon(nextStep?.instruction), null, tint = Color.White, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val toStep = nextStep?.let { step ->
                    currentLocation?.let { WalkTracker.distance(LatLng(it.latitude, it.longitude), step.location) }
                }
                if (toStep != null) {
                    Text(formatDistanceText(toStep / 1000f), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    nextStep?.instruction ?: "Sigue la línea azul",
                    color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))
            ) { Icon(Icons.Default.Close, "Terminar ruta", tint = Color.White, modifier = Modifier.size(18.dp)) }
        }

        Spacer(Modifier.height(6.dp))

        // Barra de estado: restante, llegada, velocidad, cadencia
        GlassCard(corner = 18.dp, elevation = 6.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    val km = (remainingMeters / 1000).toFloat()
                    Text(
                        "${formatWalkingTimeText(km)} · ${formatDistanceText(km)}",
                        color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                    )
                    val sub = if (route.waypoints.isNotEmpty()) {
                        val shown = (nextStopIndex + 1).coerceAtMost(route.waypoints.size)
                        "Parada $shown de ${route.waypoints.size}: ${route.waypoints.getOrNull(nextStopIndex)?.name ?: route.destinationName}"
                    } else "Hacia ${route.destinationName}"
                    Text(sub, color = c.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Pill("${speedKmh.roundToInt()} km/h", NavBlue, icon = Icons.AutoMirrored.Filled.DirectionsWalk)
                Spacer(Modifier.width(6.dp))
                Pill(if (cadenceSpm > 0) "$cadenceSpm spm" else "— spm", Mint, icon = Icons.Default.GraphicEq)
            }
        }
    }
}

private fun maneuverIcon(instruction: String?) = when {
    instruction == null -> Icons.Default.Navigation
    instruction.startsWith("Llega") -> Icons.Default.Flag
    instruction.contains("izquierda") -> Icons.Default.TurnLeft
    instruction.contains("derecha") -> Icons.Default.TurnRight
    instruction.contains("vuelta") -> Icons.Default.UTurnLeft
    instruction.contains("glorieta") -> Icons.Default.RoundaboutRight
    else -> Icons.Default.Straight
}
