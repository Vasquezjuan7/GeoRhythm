package com.example.georhythm.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import com.example.georhythm.audio.MusicPlayerEngine
import com.example.georhythm.data.*
import com.example.georhythm.network.OsrmService
import com.example.georhythm.network.SpotifyService
import com.example.georhythm.ui.components.MAX_WALKING_KM
import com.example.georhythm.ui.components.PASTO_CENTER
import com.example.georhythm.ui.components.formatDistanceText
import com.example.georhythm.ui.components.isLocationInPastoRegion
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import java.util.Locale

/**
 * Estado y lógica de la app a nivel de proceso (no de pantalla):
 * GPS, navegación, reproductor, audio-guía y guardado en Supabase.
 * Así todo sigue funcionando con la pantalla apagada (vía [WalkService]) y al girar el teléfono.
 */
class GeoRhythmController(private val app: Context) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val userManager = UserManager(app)
    val tracker = WalkTracker()
    val player = MusicPlayerEngine(app, scope, SpotifyService(app)) { source, id, title, artist, bpm ->
        scope.launch { userManager.logTrack(source.name.lowercase(), id, title, artist, bpm) }
    }

    var currentLocation by mutableStateOf<Location?>(null)
        private set
    var isCalculatingRoute by mutableStateOf(false)
        private set
    var storyPoi by mutableStateOf<PointOfInterest?>(null)
    var isNarrating by mutableStateOf(false)
        private set
    /** Se incrementa cada vez que empieza una ruta nueva (no en recálculos); la UI vuelve al mapa. */
    var routeStartCount by mutableIntStateOf(0)
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    fun notify(msg: String) { _messages.tryEmit(msg) }

    // ================================================================= GPS

    private val fused = LocationServices.getFusedLocationProviderClient(app)
    private var locationActive = false
    private var uiVisible = false
    var serviceRunning = false
        set(value) { field = value; updateLocationUpdates() }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onLocation(it) }
        }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun setUiVisible(visible: Boolean) {
        uiVisible = visible
        updateLocationUpdates()
        if (visible) maybeStartService()
    }

    /** GPS activo si la app está en pantalla o si el servicio de caminata está corriendo. */
    @SuppressLint("MissingPermission")
    fun updateLocationUpdates() {
        val want = (uiVisible || serviceRunning) && hasLocationPermission()
        if (want && !locationActive) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()
            runCatching {
                fused.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                locationActive = true
            }
        } else if (!want && locationActive) {
            fused.removeLocationUpdates(locationCallback)
            locationActive = false
            player.updateCadence(0)
        }
    }

    private fun onLocation(loc: Location) {
        currentLocation = loc
        if (!isLocationInPastoRegion(loc)) return // descarta (0,0) y lecturas fuera de Nariño
        val events = tracker.onLocation(loc, userManager.discoveredPoiIds)
        player.updateCadence(tracker.cadenceSpm)
        events.forEach { e ->
            when (e) {
                is WalkEvent.Discovered -> scope.launch {
                    userManager.markPoiDiscovered(e.poi.id)
                    if (tracker.route == null) notify("Descubriste: ${e.poi.name}")
                }
                is WalkEvent.ReachedStop -> {
                    storyPoi = e.poi
                    scope.launch { userManager.markPoiDiscovered(e.poi.id) }
                    if (e.poi.story != null && !isNarrating) narrate(e.poi)
                    else notify("Parada ${e.index + 1} de ${e.total}: ${e.poi.name}")
                }
                is WalkEvent.Arrived -> finishNavigation(arrived = true)
                WalkEvent.OffRoute -> tracker.route?.let { r ->
                    startNavigation(r.destinationLatLng, r.destinationName, r.waypoints, r.routeId, reroute = true)
                }
            }
        }
    }

    // ================================================================= SERVICIO EN PRIMER PLANO

    init {
        // Arranca el servicio cuando hay ruta activa o música sonando (sólo con la app visible:
        // Android no permite iniciar servicios de ubicación desde segundo plano)
        scope.launch {
            snapshotFlow { tracker.route != null || player.isPlaying }
                .distinctUntilChanged()
                .collect { if (it) maybeStartService() }
        }
    }

    val isWalkActive: Boolean get() = tracker.route != null || player.isPlaying

    private fun maybeStartService() {
        if (uiVisible && !serviceRunning && isWalkActive) WalkService.start(app)
    }

    // ================================================================= AUDIO-GUÍA

    private val mainHandler = Handler(Looper.getMainLooper())
    private val tts: TextToSpeech by lazy {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) engine.language = Locale.forLanguageTag("es-CO")
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { mainHandler.post { isNarrating = true; player.duck(true) } }
            override fun onDone(id: String?) { mainHandler.post { isNarrating = false; player.duck(false) } }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { mainHandler.post { isNarrating = false; player.duck(false) } }
        })
        engine
    }

    fun narrate(poi: PointOfInterest) {
        if (isNarrating) stopNarration()
        else tts.speak(poi.story ?: "${poi.name}. ${poi.description}", TextToSpeech.QUEUE_FLUSH, null, "poi_${poi.id}")
    }

    fun stopNarration() {
        tts.stop()
        isNarrating = false
        player.duck(false)
    }

    fun dismissStory() {
        if (isNarrating) stopNarration()
        storyPoi = null
    }

    // ================================================================= NAVEGACIÓN

    fun finishNavigation(arrived: Boolean) {
        val routeId = tracker.route?.routeId
        val record = tracker.stopRoute()
        if (record != null) {
            scope.launch {
                val ok = userManager.saveWalk(record, System.currentTimeMillis(), routeId)
                notify(
                    (if (arrived) "¡Llegaste! " else "Ruta terminada. ") +
                        "${formatDistanceText((record.distanceMeters / 1000).toFloat())} guardados" +
                        if (ok) "." else " (sin conexión, no se pudo sincronizar)."
                )
            }
        } else if (arrived) notify("¡Llegaste a tu destino!")
    }

    fun startNavigation(
        dest: LatLng,
        name: String,
        waypoints: List<PointOfInterest> = emptyList(),
        routeId: String? = null,
        reroute: Boolean = false
    ) {
        val loc = currentLocation
        val firstTarget = (if (reroute) waypoints.getOrNull(tracker.nextStopIndex) else waypoints.firstOrNull())?.latLng ?: dest
        val userPoint = if (isLocationInPastoRegion(loc)) LatLng(loc!!.latitude, loc.longitude) else null
        val start = when {
            // Lejos del inicio de una ruta recomendada → empieza en su primera parada
            userPoint != null && waypoints.isNotEmpty() && WalkTracker.distance(userPoint, firstTarget) > 3_000 -> firstTarget
            userPoint != null -> userPoint
            waypoints.isNotEmpty() -> firstTarget
            else -> PASTO_CENTER
        }
        if (WalkTracker.distance(start, dest) / 1000f > MAX_WALKING_KM) {
            notify("$name está demasiado lejos para ir caminando.")
            return
        }
        if (!reroute) isCalculatingRoute = true
        scope.launch {
            // Al recalcular sólo se piden las paradas que faltan; el progreso se conserva
            val pending = if (reroute) waypoints.drop(tracker.nextStopIndex) else waypoints
            val stopsForRequest = pending.filter { WalkTracker.distance(it.latLng, start) > 15 }
            val route = OsrmService.fetchWalkingRoute(start, dest, name, stopsForRequest, routeId)
            isCalculatingRoute = false
            if (route == null) {
                if (!reroute) notify("No se pudo calcular la ruta. Revisa tu conexión.")
                return@launch
            }
            tracker.startRoute(route.copy(waypoints = waypoints), keepSession = reroute)
            if (reroute) notify("Ruta recalculada")
            else {
                routeStartCount++
                if (userPoint == null) notify("Sin GPS: la ruta empieza en ${waypoints.firstOrNull()?.name ?: "la Plaza de Nariño"}.")
            }
        }
    }

    fun startRecommendedRoute(route: HistoricalRoute) {
        val stops = LocalData.stopsOf(route)
        if (stops.isEmpty()) return
        if (tracker.route != null) finishNavigation(false)
        startNavigation(stops.last().latLng, route.title, stops, route.id)
        if (player.rhythmSyncEnabled && !player.isPlaying) player.startPaceMix()
    }

    /** Detiene todo (desde la notificación o al cerrar sesión). */
    fun stopEverything() {
        if (tracker.route != null) finishNavigation(false)
        if (player.isPlaying) player.togglePlayPause()
        if (isNarrating) stopNarration()
    }

    fun onLogout() {
        tracker.stopRoute()
        if (player.isPlaying) player.togglePlayPause()
        storyPoi = null
    }
}
