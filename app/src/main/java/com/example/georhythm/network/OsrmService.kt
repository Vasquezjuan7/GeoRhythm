package com.example.georhythm.network

import com.example.georhythm.data.NavigationRouteData
import com.example.georhythm.data.PointOfInterest
import com.example.georhythm.data.RouteStep
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng

object OsrmService {

    /**
     * Servidor OSRM de FOSSGIS con perfil PEATONAL real. El servidor demo de project-osrm.org
     * ignora "/foot/" y siempre calcula rutas en carro (respeta sentidos viales y da tiempos de carro),
     * por eso las rutas anteriores eran incorrectas. Se mantiene sólo como respaldo.
     */
    private const val FOOT_SERVER = "https://routing.openstreetmap.de/routed-foot/route/v1/driving"
    private const val FALLBACK_SERVER = "https://router.project-osrm.org/route/v1/foot"

    /** Ruta a pie desde [start] pasando por [waypoints] (en orden) hasta [dest]. */
    suspend fun fetchWalkingRoute(
        start: LatLng,
        dest: LatLng,
        destName: String,
        waypoints: List<PointOfInterest> = emptyList(),
        routeId: String? = null
    ): NavigationRouteData? {
        val coords = (listOf(start) + waypoints.map { it.latLng }.filter { it != dest } + dest)
            .joinToString(";") { "${it.longitude},${it.latitude}" }
        val query = "$coords?overview=full&geometries=geojson&steps=true"
        val res = Http.request("$FOOT_SERVER/$query", timeoutMs = 15_000)
            .takeIf { it.ok }
            ?: Http.request("$FALLBACK_SERVER/$query", timeoutMs = 15_000)
        if (!res.ok) return null
        return runCatching { parse(JSONObject(res.body), dest, destName, waypoints, routeId) }.getOrNull()
    }

    private fun parse(
        json: JSONObject,
        dest: LatLng,
        destName: String,
        waypoints: List<PointOfInterest>,
        routeId: String?
    ): NavigationRouteData? {
        if (json.optString("code") != "Ok") return null
        val route = json.getJSONArray("routes").optJSONObject(0) ?: return null

        val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
        val points = (0 until coords.length()).map {
            val pt = coords.getJSONArray(it)
            LatLng(pt.getDouble(1), pt.getDouble(0))
        }

        val steps = mutableListOf<RouteStep>()
        val legs = route.getJSONArray("legs")
        var searchFrom = 0
        for (l in 0 until legs.length()) {
            val legSteps = legs.getJSONObject(l).getJSONArray("steps")
            val legTarget = waypoints.getOrNull(l)?.name ?: destName
            for (s in 0 until legSteps.length()) {
                val st = legSteps.getJSONObject(s)
                val m = st.getJSONObject("maneuver")
                val loc = m.getJSONArray("location").let { LatLng(it.getDouble(1), it.getDouble(0)) }
                val type = m.optString("type")
                // Evitar indicaciones redundantes entre tramos
                if (type == "depart" && l > 0) continue
                if (type == "arrive" && l < legs.length() - 1) {
                    val idx = nearestIndex(points, loc, searchFrom)
                    steps += RouteStep("Llegas a $legTarget", 0.0, loc, idx)
                    searchFrom = idx
                    continue
                }
                val idx = nearestIndex(points, loc, searchFrom)
                searchFrom = idx
                steps += RouteStep(
                    instruction(type, m.optString("modifier"), st.optString("name").trim(), legTarget),
                    st.optDouble("distance", 0.0), loc, idx
                )
            }
        }

        return NavigationRouteData(
            destinationName = destName,
            destinationLatLng = dest,
            points = points,
            totalDistanceMeters = route.getDouble("distance"),
            // Estimación peatonal propia (4.8 km/h) para que no dependa del servidor
            totalDurationSeconds = route.getDouble("distance") / (4.8 / 3.6),
            steps = steps,
            waypoints = waypoints,
            routeId = routeId
        )
    }

    private fun instruction(type: String, modifier: String, street: String, target: String): String {
        val on = if (street.isNotEmpty()) " por $street" else ""
        val dir = when (modifier) {
            "left" -> "Gira a la izquierda"
            "right" -> "Gira a la derecha"
            "slight left" -> "Mantente a la izquierda"
            "slight right" -> "Mantente a la derecha"
            "sharp left" -> "Gira cerrado a la izquierda"
            "sharp right" -> "Gira cerrado a la derecha"
            "uturn" -> "Da la vuelta"
            else -> "Sigue derecho"
        }
        return when (type) {
            "depart" -> "Comienza a caminar$on"
            "arrive" -> "Llegaste a $target"
            "roundabout", "rotary" -> "Toma la glorieta$on"
            "continue", "new name" -> if (modifier.contains("left") || modifier.contains("right")) "$dir$on" else "Continúa$on"
            else -> "$dir$on"
        }
    }

    private fun nearestIndex(points: List<LatLng>, target: LatLng, from: Int): Int {
        var best = from
        var bestD = Double.MAX_VALUE
        for (i in from until points.size) {
            val d = points[i].distanceTo(target)
            if (d < bestD) { bestD = d; best = i }
            if (bestD < 1.0) break
        }
        return best
    }
}
