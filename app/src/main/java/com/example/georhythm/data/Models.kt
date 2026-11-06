package com.example.georhythm.data

import org.maplibre.android.geometry.LatLng

// --- USUARIO ---
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val runnerType: String = "Explorador Urbano",
    val avatarEmoji: String = "👟",
    val registeredAt: Long = System.currentTimeMillis()
)

data class UserStats(
    val totalKm: Float = 0f,
    val totalWalks: Int = 0,
    val totalMinutes: Int = 0,
    val discoveredPois: Int = 0
)

data class WalkRecord(
    val startedAt: Long,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val avgSpeedKmh: Float,
    val avgCadence: Int,
    val destinationName: String?
)

data class FavoritePlace(val id: String, val name: String, val lat: Double, val lon: Double) {
    val latLng: LatLng get() = LatLng(lat, lon)
}

// --- MAPA ---
data class CityRegion(
    val id: String,
    val name: String,
    val center: LatLng,
    val zoom: Double = 15.0
)

enum class MapLayerStyle(val styleName: String, val displayName: String) {
    STREETS("streets-v2", "Calles"),
    SATELLITE("hybrid", "Satélite"),
    TERRAIN("outdoor-v2", "Relieve"),
    NIGHT("streets-v2-dark", "Noche")
}

/** Categoría de un lugar: define el color del punto en el mapa. */
enum class PoiCategory(val label: String, val colorHex: String) {
    HISTORIA("Historia", "#B45309"),
    TEMPLO("Templo", "#7C3AED"),
    CULTURA("Cultura", "#DB2777"),
    PARQUE("Parque", "#16A34A"),
    COMERCIO("Comercio", "#2563EB"),
    COMIDA("Comida", "#EA580C"),
    EDUCACION("Educación", "#0891B2"),
    SALUD("Salud", "#DC2626"),
    TRANSPORTE("Transporte", "#475569"),
    NATURALEZA("Naturaleza", "#0D9488")
}

data class PointOfInterest(
    val id: String,
    val name: String,
    val address: String,
    val latLng: LatLng,
    val category: PoiCategory,
    val description: String = "",
    /** Relato corto para la audio-guía al llegar al lugar. */
    val story: String? = null
)

// --- RUTAS RECOMENDADAS (RECORRIDOS A PIE CON PARADAS) ---
data class HistoricalRoute(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val tagColorHex: String,
    /** Paradas en orden; la ruta pasa por todas. */
    val stopIds: List<String>,
    val suggestedMood: String
)

data class NavigationDestination(
    val name: String,
    val category: String,
    val latLng: LatLng
)

data class RouteStep(
    val instruction: String,
    val distanceMeters: Double,
    val location: LatLng,
    /** Índice del punto de la polilínea donde ocurre la maniobra. */
    val pointIndex: Int
)

data class NavigationRouteData(
    val destinationName: String,
    val destinationLatLng: LatLng,
    val points: List<LatLng>,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double,
    val steps: List<RouteStep>,
    /** Paradas intermedias (para rutas recomendadas). */
    val waypoints: List<PointOfInterest> = emptyList(),
    val routeId: String? = null
)

// --- MÚSICA ---
data class SpotifyPlaylist(
    val name: String,
    val uri: String,
    val imageUrl: String?,
    val tracksCount: Int = 0
)

data class SpotifyTrack(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val durationMs: Long
)

data class RadioStation(
    val id: String,
    val name: String,
    val genre: String,
    val targetBpm: Int,
    val streamUrl: String,
    val coverGradientHex: Pair<String, String>
)

/** Canción de Audius (catálogo abierto, streaming completo y BPM real). */
data class AudiusTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val artworkUrl: String?,
    val genre: String,
    /** BPM detectado por Audius; null si no está disponible. */
    val bpm: Int?
) {
    val streamUrl: String get() = "https://api.audius.co/v1/tracks/$id/stream?app_name=GeoRhythm"
}
