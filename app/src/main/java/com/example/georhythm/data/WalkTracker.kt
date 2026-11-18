package com.example.georhythm.data

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.georhythm.util.GeoMath
import org.maplibre.android.geometry.LatLng
import kotlin.math.roundToInt

/** Eventos que la UI debe atender (audio-guía, llegada, recalcular ruta). */
sealed class WalkEvent {
    data class ReachedStop(val poi: PointOfInterest, val index: Int, val total: Int) : WalkEvent()
    data class Arrived(val record: WalkRecord, val routeId: String?) : WalkEvent()
    data object OffRoute : WalkEvent()
    data class Discovered(val poi: PointOfInterest) : WalkEvent()
}

/**
 * Procesa las lecturas GPS:
 * - Velocidad suavizada (filtra ruido y lecturas imprecisas) y cadencia estimada en pasos/min.
 * - Progreso de navegación: distancia restante, próxima indicación, paradas alcanzadas, desvíos.
 * - Registro de la caminata (distancia, duración, promedios) para guardarla en Supabase.
 */
class WalkTracker(private val clock: () -> Long = System::currentTimeMillis) {

    var speedKmh by mutableFloatStateOf(0f)
        private set
    var cadenceSpm by mutableIntStateOf(0)
        private set

    // Navegación
    var route by mutableStateOf<NavigationRouteData?>(null)
        private set
    var remainingMeters by mutableDoubleStateOf(0.0)
        private set
    var nextInstruction by mutableStateOf<RouteStep?>(null)
        private set
    var nextStopIndex by mutableIntStateOf(0)
        private set

    // Sesión de caminata
    var walkDistanceMeters by mutableDoubleStateOf(0.0)
        private set
    var walkStartedAt by mutableStateOf<Long?>(null)
        private set

    private var lastFix: GpsFix? = null
    private val speedFilter = SpeedFilter()
    private var cadenceSum = 0L
    private var cadenceSamples = 0
    private var offRouteCount = 0
    private var lastRerouteAt = 0L

    fun startRoute(data: NavigationRouteData, keepSession: Boolean = false) {
        route = data
        remainingMeters = data.totalDistanceMeters
        nextInstruction = data.steps.firstOrNull { it.distanceMeters > 0 } ?: data.steps.firstOrNull()
        offRouteCount = 0
        if (!keepSession) {
            nextStopIndex = 0
            walkStartedAt = clock()
            walkDistanceMeters = 0.0
            cadenceSum = 0
            cadenceSamples = 0
        }
    }

    /** Termina la navegación y devuelve el registro si hubo una caminata real (> 50 m). */
    fun stopRoute(): WalkRecord? {
        val record = buildRecord()
        route = null
        nextInstruction = null
        walkStartedAt = null
        return record
    }

    private fun buildRecord(): WalkRecord? {
        val start = walkStartedAt ?: return null
        if (walkDistanceMeters < 50) return null
        val durS = ((clock() - start) / 1000).toInt().coerceAtLeast(1)
        return WalkRecord(
            startedAt = start,
            distanceMeters = walkDistanceMeters,
            durationSeconds = durS,
            avgSpeedKmh = ((walkDistanceMeters / durS) * 3.6).toFloat(),
            avgCadence = if (cadenceSamples > 0) (cadenceSum / cadenceSamples).toInt() else 0,
            destinationName = route?.destinationName
        )
    }

    fun onLocation(loc: Location, discovered: Set<String>): List<WalkEvent> = onFix(loc.toGpsFix(), discovered)

    fun onFix(fix: GpsFix, discovered: Set<String>): List<WalkEvent> {
        val events = mutableListOf<WalkEvent>()
        if (fix.accuracyM != null && fix.accuracyM > 40f) return events // lectura demasiado imprecisa

        // ---- Velocidad suavizada y cadencia
        val kmh = speedFilter.update(fix)
        speedKmh = kmh
        cadenceSpm = cadenceFor(kmh)

        // ---- Distancia de la sesión
        val prev = lastFix
        val here = LatLng(fix.latitude, fix.longitude)
        if (walkStartedAt != null && prev != null) {
            val d = distance(LatLng(prev.latitude, prev.longitude), here)
            if (d in 2f..60f) walkDistanceMeters += d
            if (cadenceSpm > 0) { cadenceSum += cadenceSpm; cadenceSamples++ }
        }
        lastFix = fix

        // ---- Lugares descubiertos (a menos de 40 m)
        LocalData.pois.forEach { poi ->
            if (poi.id !in discovered && distance(here, poi.latLng) < 40f) events += WalkEvent.Discovered(poi)
        }

        // ---- Progreso de navegación
        val r = route ?: return events
        val (nearestIdx, nearestDist) = nearestPoint(r.points, here)
        remainingMeters = remainingFrom(r.points, nearestIdx, here)
        nextInstruction = r.steps.firstOrNull { it.pointIndex > nearestIdx && distance(here, it.location) > 10f }
            ?: r.steps.lastOrNull()

        // Paradas de la ruta recomendada
        if (r.waypoints.isNotEmpty() && nextStopIndex < r.waypoints.size) {
            val stop = r.waypoints[nextStopIndex]
            if (distance(here, stop.latLng) < 35f) {
                events += WalkEvent.ReachedStop(stop, nextStopIndex, r.waypoints.size)
                nextStopIndex++
            }
        }

        // Llegada
        val isLastStopDone = r.waypoints.isEmpty() || nextStopIndex >= r.waypoints.size
        if (isLastStopDone && distance(here, r.destinationLatLng) < 25f) {
            val record = buildRecord() ?: WalkRecord(
                walkStartedAt ?: clock(), walkDistanceMeters, 0, 0f, 0, r.destinationName
            )
            events += WalkEvent.Arrived(record, r.routeId)
            return events
        }

        // Desvío: > 45 m de la ruta en 3 lecturas seguidas (máx. 1 recálculo cada 20 s)
        if (nearestDist > 45f) offRouteCount++ else offRouteCount = 0
        val now = clock()
        if (offRouteCount >= 3 && now - lastRerouteAt > 20_000) {
            offRouteCount = 0
            lastRerouteAt = now
            events += WalkEvent.OffRoute
        }
        return events
    }

    /** Paradas que aún faltan (para recalcular la ruta sin repetir las ya visitadas). */
    fun remainingStops(): List<PointOfInterest> = route?.waypoints?.drop(nextStopIndex) ?: emptyList()

    companion object {
        /**
         * Cadencia aproximada (pasos/min) a partir de la velocidad.
         * 3 km/h ≈ 97 spm, 5 km/h ≈ 119 spm, 6 km/h ≈ 130 spm, trote 9 km/h ≈ 160 spm.
         */
        fun cadenceFor(kmh: Float): Int =
            if (kmh < 1f) 0 else (64 + 11 * kmh).roundToInt().coerceAtMost(185)

        fun distance(a: LatLng, b: LatLng): Float =
            GeoMath.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude).toFloat()

        private fun nearestPoint(points: List<LatLng>, here: LatLng): Pair<Int, Float> {
            var best = 0
            var bestD = Float.MAX_VALUE
            points.forEachIndexed { i, p ->
                val d = distance(here, p)
                if (d < bestD) { bestD = d; best = i }
            }
            return best to bestD
        }

        private fun remainingFrom(points: List<LatLng>, idx: Int, here: LatLng): Double {
            if (points.isEmpty()) return 0.0
            var total = distance(here, points[idx]).toDouble()
            for (i in idx until points.size - 1) total += distance(points[i], points[i + 1])
            return total
        }
    }
}
