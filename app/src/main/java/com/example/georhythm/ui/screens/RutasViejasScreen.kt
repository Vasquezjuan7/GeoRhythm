package com.example.georhythm.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.georhythm.data.HistoricalRoute
import com.example.georhythm.data.LocalData
import com.example.georhythm.data.WalkTracker
import com.example.georhythm.ui.components.*
import com.example.georhythm.ui.theme.*

/** Rutas recomendadas: recorridos a pie con paradas en orden y audio-guía. */
@Composable
fun RutasViejasScreen(
    currentLocation: Location?,
    discoveredIds: Set<String>,
    onPreview: (HistoricalRoute) -> Unit,
    onStartRoute: (HistoricalRoute) -> Unit
) {
    val c = LocalGeoColors.current
    var filter by remember { mutableStateOf<String?>(null) }
    val tags = remember { LocalData.historicalRoutes.map { it.tag }.distinct() }
    val routes = LocalData.historicalRoutes.filter { filter == null || it.tag == filter }

    LazyColumn(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Rutas", "Recorridos a pie por Pasto con audio-guía en cada parada") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todas") })
                tags.take(3).forEach { t ->
                    FilterChip(selected = filter == t, onClick = { filter = if (filter == t) null else t },
                        label = { Text(t.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1) })
                }
            }
        }
        items(routes, key = { it.id }) { route ->
            RouteCard(route, currentLocation, discoveredIds, onPreview, onStartRoute)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RouteCard(
    route: HistoricalRoute,
    loc: Location?,
    discoveredIds: Set<String>,
    onPreview: (HistoricalRoute) -> Unit,
    onStart: (HistoricalRoute) -> Unit
) {
    val c = LocalGeoColors.current
    val stops = remember(route) { LocalData.stopsOf(route) }
    // Distancia del recorrido: tramos entre paradas × 1.3 (factor de calles reales)
    val circuitKm = remember(route) {
        stops.zipWithNext { a, b -> WalkTracker.distance(a.latLng, b.latLng) }.sum() * 1.3f / 1000f
    }
    val toStartKm = calculateDistanceKm(loc, stops.first().latLng)
    val found = stops.count { it.id in discoveredIds }
    val tagColor = parseHexColor(route.tagColorHex)

    GlassCard(Modifier.fillMaxWidth(), elevation = 4.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(route.tag, tagColor)
                Spacer(Modifier.weight(1f))
                if (found > 0) Pill("$found/${stops.size} descubiertas", Emerald, icon = Icons.Default.Check)
            }
            Spacer(Modifier.height(8.dp))
            Text(route.title, color = c.text, style = MaterialTheme.typography.titleLarge)
            Text(route.subtitle, color = c.textMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(12.dp))
            // Paradas en orden
            stops.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .background(if (s.id in discoveredIds) Emerald else c.surfaceAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = if (s.id in discoveredIds) c.onAccent else c.textMuted)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(s.name, color = c.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (s.story != null) Icon(Icons.Default.Headphones, "Con audio-guía", tint = Gold, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("${formatDistanceText(circuitKm)} · ${formatWalkingTimeText(circuitKm)}", NavBlue, icon = Icons.Default.Route)
                Pill(route.suggestedMood, Violet, icon = Icons.Default.MusicNote)
            }
            if (toStartKm != null) {
                Text(
                    "Inicio a ${formatDistanceText(toStartKm)} de ti",
                    color = c.textMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onPreview(route) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Map, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver en mapa")
                }
                Button(
                    onClick = { onStart(route) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = c.onAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Iniciar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
