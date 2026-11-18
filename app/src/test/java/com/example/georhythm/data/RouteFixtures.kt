package com.example.georhythm.data

import org.maplibre.android.geometry.LatLng

/** Ruta recta hacia el norte desde la Plaza de Nariño: un punto cada ~11 m. */
object RouteFixtures {
    const val START_LAT = 1.214611
    const val LON = -77.278280
    const val STEP_DEG = 0.0001 // ~11.1 m

    fun straightRoute(points: Int = 40, waypoints: List<PointOfInterest> = emptyList()): NavigationRouteData {
        val pts = (0 until points).map { LatLng(START_LAT + it * STEP_DEG, LON) }
        return NavigationRouteData(
            destinationName = "Destino de prueba",
            destinationLatLng = pts.last(),
            points = pts,
            totalDistanceMeters = (points - 1) * 11.1,
            totalDurationSeconds = 0.0,
            steps = listOf(
                RouteStep("Comienza a caminar", 100.0, pts[0], 0),
                RouteStep("Gira a la derecha por Calle 18", 100.0, pts[points / 2], points / 2),
                RouteStep("Llegaste a Destino de prueba", 0.0, pts.last(), points - 1)
            ),
            waypoints = waypoints,
            routeId = "test"
        )
    }

    fun stop(id: String, index: Int) = PointOfInterest(
        id, "Parada $id", "", LatLng(START_LAT + index * STEP_DEG, LON), PoiCategory.HISTORIA
    )

    /** Lectura GPS precisa sobre la ruta, en el punto [index]. */
    fun fixAt(index: Int, timeMs: Long, lonOffset: Double = 0.0) =
        GpsFix(START_LAT + index * STEP_DEG, LON + lonOffset, accuracyM = 5f, speedMps = 1.4f, timeMs = timeMs)
}
