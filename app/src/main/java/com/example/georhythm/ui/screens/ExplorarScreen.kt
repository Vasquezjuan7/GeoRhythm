package com.example.georhythm.ui.screens

import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.georhythm.audio.MusicPlayerEngine
import com.example.georhythm.data.FavoritePlace
import com.example.georhythm.data.LocalData
import com.example.georhythm.data.MapLayerStyle
import com.example.georhythm.data.PointOfInterest
import com.example.georhythm.data.WalkTracker
import com.example.georhythm.ui.components.*
import com.example.georhythm.ui.theme.*
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

@Composable
fun ExplorarScreen(
    currentLocation: Location?,
    hasLocationPermission: Boolean,
    tracker: WalkTracker,
    player: MusicPlayerEngine,
    isDark: Boolean,
    discoveredIds: Set<String>,
    favorites: List<FavoritePlace>,
    previewStops: List<PointOfInterest>,
    storyPoi: PointOfInterest?,
    isNarrating: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSearch: () -> Unit,
    onNavigateTo: (PointOfInterest) -> Unit,
    onCancelNavigation: () -> Unit,
    onOpenMusic: () -> Unit,
    onToggleFavorite: (PointOfInterest) -> Unit,
    onNarrate: (PointOfInterest) -> Unit,
    onDismissStory: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val c = LocalGeoColors.current
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var isAutoTracking by remember { mutableStateOf(true) }
    var mapStyle by remember { mutableStateOf(if (isDark) MapLayerStyle.NIGHT else MapLayerStyle.STREETS) }
    var userPickedStyle by remember { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<PointOfInterest?>(null) }
    val route = tracker.route

    // El mapa sigue el tema día/noche salvo que el usuario elija otra capa
    LaunchedEffect(isDark) {
        if (!userPickedStyle || mapStyle == MapLayerStyle.NIGHT || mapStyle == MapLayerStyle.STREETS) {
            mapStyle = if (isDark) MapLayerStyle.NIGHT else MapLayerStyle.STREETS
        }
    }

    // Al iniciar una ruta: vista general y luego seguimiento
    LaunchedEffect(route) {
        val m = map ?: return@LaunchedEffect
        if (route != null && route.points.size > 1) {
            isAutoTracking = false
            val b = LatLngBounds.Builder().apply { route.points.forEach { include(it) } }.build()
            m.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 140, 420, 140, 520), 900)
            kotlinx.coroutines.delay(2500)
            isAutoTracking = true
        }
    }

    // Vista previa de una ruta recomendada: encuadrar sus paradas
    LaunchedEffect(previewStops) {
        val m = map ?: return@LaunchedEffect
        if (previewStops.size > 1 && route == null) {
            isAutoTracking = false
            val b = LatLngBounds.Builder().apply { previewStops.forEach { include(it.latLng) } }.build()
            m.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 160, 360, 160, 460), 900)
        }
    }

    Box(Modifier.fillMaxSize()) {
        MapLibreMapView(
            modifier = Modifier.fillMaxSize(),
            mapLayerStyle = mapStyle,
            hasLocationPermission = hasLocationPermission,
            currentLocation = currentLocation,
            activeRoute = route,
            previewStops = previewStops,
            discoveredIds = discoveredIds,
            isAutoTracking = isAutoTracking,
            isNavigating = route != null,
            onUserGesturedMap = { isAutoTracking = false; showLayers = false },
            onMapReady = { map = it },
            onPoiSelected = { selectedPoi = it; showLayers = false }
        )

        // ------------------------------------------------------------- PARTE SUPERIOR
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
            if (route != null) {
                NavigationHUD(
                    route = route,
                    nextStep = tracker.nextInstruction,
                    remainingMeters = tracker.remainingMeters,
                    nextStopIndex = tracker.nextStopIndex,
                    currentLocation = currentLocation,
                    speedKmh = tracker.speedKmh,
                    cadenceSpm = tracker.cadenceSpm,
                    onCancel = onCancelNavigation
                )
            } else {
                GlassCard(Modifier.padding(horizontal = 12.dp).fillMaxWidth(), corner = 28.dp, onClick = onOpenSearch) {
                    Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Emerald, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("¿A dónde quieres caminar?", color = c.textMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        PaceBadge(tracker.speedKmh, tracker.cadenceSpm)
                        IconButton(onClick = onToggleTheme) {
                            Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, "Cambiar tema", tint = c.text, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(LocalData.regions) { region ->
                        Text(
                            region.name,
                            color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(c.glass)
                                .border(1.dp, c.border, RoundedCornerShape(50))
                                .clickable {
                                    isAutoTracking = false
                                    map?.animateCamera(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.Builder().target(region.center).zoom(region.zoom).tilt(30.0).build()
                                        ), 1200
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            if (!hasLocationPermission) {
                GlassCard(Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth(), corner = 16.dp, onClick = onRequestPermission) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOff, null, tint = Gold)
                        Spacer(Modifier.width(10.dp))
                        Text("Activa la ubicación para navegar y sincronizar la música con tu paso", color = c.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("Activar", color = Emerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // ------------------------------------------------------------- CONTROLES LATERALES
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapButton(Icons.Default.Layers, "Capas", c.text) { showLayers = !showLayers }
            MapButton(Icons.Default.Add, "Acercar", c.text) { map?.animateCamera(CameraUpdateFactory.zoomIn(), 300) }
            MapButton(Icons.Default.Remove, "Alejar", c.text) { map?.animateCamera(CameraUpdateFactory.zoomOut(), 300) }
            if (route != null) {
                MapButton(Icons.Default.Route, "Ver ruta completa", c.text) {
                    isAutoTracking = false
                    val b = LatLngBounds.Builder().apply { route.points.forEach { include(it) } }.build()
                    map?.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 140, 420, 140, 520), 800)
                }
            }
            MapButton(
                if (isAutoTracking) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                "Centrar en mi ubicación",
                if (isAutoTracking) NavBlue else c.text
            ) {
                isAutoTracking = true
                val loc = currentLocation
                val target = if (isLocationInPastoRegion(loc)) LatLng(loc!!.latitude, loc.longitude) else PASTO_CENTER
                map?.animateCamera(
                    CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(target).zoom(17.5).tilt(60.0).bearing(0.0).build()),
                    800
                )
            }
        }

        if (showLayers) {
            GlassCard(Modifier.align(Alignment.CenterEnd).padding(end = 64.dp), corner = 18.dp) {
                Column(Modifier.padding(8.dp).width(150.dp)) {
                    MapLayerStyle.entries.forEach { style ->
                        val sel = style == mapStyle
                        Text(
                            style.displayName,
                            color = if (sel) NavBlue else c.text,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) NavBlue.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { mapStyle = style; userPickedStyle = true; showLayers = false }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // ------------------------------------------------------------- PARTE INFERIOR
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 8.dp)) {
            AnimatedVisibility(
                visible = storyPoi != null,
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()
            ) {
                storyPoi?.let { StoryCard(it, isNarrating, onNarrate, onDismissStory) }
            }
            AnimatedVisibility(
                visible = selectedPoi != null && storyPoi == null,
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()
            ) {
                selectedPoi?.let { poi ->
                    PoiCard(
                        poi = poi,
                        currentLocation = currentLocation,
                        isFavorite = favorites.any { it.name == poi.name },
                        isDiscovered = poi.id in discoveredIds,
                        onClose = { selectedPoi = null },
                        onNavigate = { onNavigateTo(poi); selectedPoi = null },
                        onFavorite = { onToggleFavorite(poi) },
                        onOpenExternal = {
                            val uri = Uri.parse("geo:${poi.latLng.latitude},${poi.latLng.longitude}?q=${poi.latLng.latitude},${poi.latLng.longitude}(${Uri.encode(poi.name)})")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            MiniMusicPlayer(player, onOpenMusic)
        }
    }
}

@Composable
private fun PaceBadge(speedKmh: Float, cadence: Int) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Emerald.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = Emerald, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            if (cadence > 0) "${speedKmh.toInt()} km/h · $cadence" else "Quieto",
            color = Emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MapButton(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    val c = LocalGeoColors.current
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(c.glass)
            .border(1.dp, c.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun PoiCard(
    poi: PointOfInterest,
    currentLocation: Location?,
    isFavorite: Boolean,
    isDiscovered: Boolean,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
    onFavorite: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val c = LocalGeoColors.current
    val km = calculateDistanceKm(currentLocation, poi.latLng)
        ?: WalkTracker.distance(PASTO_CENTER, poi.latLng) / 1000f
    val tooFar = km > MAX_WALKING_KM
    val catColor = parseHexColor(poi.category.colorHex)

    GlassCard(Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(poi.category.label, catColor)
                        if (isDiscovered) Pill("Descubierto", Emerald, icon = Icons.Default.Check)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(poi.name, color = c.text, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(poi.address, color = c.textMuted, fontSize = 12.sp)
                }
                IconButton(onClick = onFavorite, Modifier.size(36.dp)) {
                    Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, "Favorito", tint = Gold)
                }
                IconButton(onClick = onClose, Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Cerrar", tint = c.textMuted)
                }
            }
            if (poi.description.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(poi.description, color = c.textMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatDistanceText(km), color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        if (tooFar) "Demasiado lejos para ir a pie" else "${formatWalkingTimeText(km)} caminando",
                        color = if (tooFar) Gold else c.textMuted, fontSize = 12.sp
                    )
                }
                if (tooFar) {
                    OutlinedButton(onClick = onOpenExternal, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Map, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir en mapas")
                    }
                } else {
                    Button(
                        onClick = onNavigate,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ir caminando", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryCard(poi: PointOfInterest, isNarrating: Boolean, onNarrate: (PointOfInterest) -> Unit, onDismiss: () -> Unit) {
    val c = LocalGeoColors.current
    GlassCard(Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("AUDIO-GUÍA", Gold, icon = Icons.Default.Headphones)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Cerrar", tint = c.textMuted) }
            }
            Text(poi.name, color = c.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
            Text(
                poi.story ?: poi.description, color = c.textMuted, fontSize = 13.sp, lineHeight = 18.sp,
                maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onNarrate(poi) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isNarrating) Gold else Emerald, contentColor = c.onAccent)
            ) {
                Icon(if (isNarrating) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isNarrating) "Detener" else "Escuchar historia", fontWeight = FontWeight.Bold)
            }
        }
    }
}
