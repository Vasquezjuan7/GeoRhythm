package com.example.georhythm.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Gestiona la cuenta del caminante y la persistencia en Supabase.
 * - Cuenta real: datos en Supabase (perfil, caminatas, lugares descubiertos, favoritos, historial musical).
 * - Invitado: los datos se guardan sólo en el teléfono.
 */
class UserManager(context: Context) {

    val supabase = SupabaseClient(context)
    private val prefs = context.getSharedPreferences("georhythm_user_prefs", Context.MODE_PRIVATE)

    var profile by mutableStateOf<UserProfile?>(null)
        private set
    var stats by mutableStateOf(UserStats())
        private set
    var recentWalks by mutableStateOf<List<WalkRecord>>(emptyList())
        private set
    var discoveredPoiIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var favorites by mutableStateOf<List<FavoritePlace>>(emptyList())
        private set
    var isSyncing by mutableStateOf(false)
        private set

    val isGuest: Boolean get() = prefs.getBoolean(KEY_GUEST, false) && supabase.session == null

    fun isLoggedIn(): Boolean = supabase.session != null || prefs.getBoolean(KEY_GUEST, false)

    // ------------------------------------------------------------- AUTENTICACIÓN

    suspend fun login(email: String, password: String): AuthOutcome {
        val out = supabase.signIn(email.trim().lowercase(), password)
        if (out is AuthOutcome.Success) {
            prefs.edit().putBoolean(KEY_GUEST, false).apply()
            refresh()
        }
        return out
    }

    suspend fun register(name: String, email: String, password: String, runnerType: String, avatarEmoji: String): AuthOutcome {
        val meta = JSONObject()
            .put("name", name.trim())
            .put("runner_type", runnerType)
            .put("avatar_emoji", avatarEmoji)
        val out = supabase.signUp(email.trim().lowercase(), password, meta)
        if (out is AuthOutcome.Success) {
            prefs.edit().putBoolean(KEY_GUEST, false).apply()
            refresh()
        }
        return out
    }

    suspend fun loginFromEmailLink(fragment: String): AuthOutcome {
        val out = supabase.signInFromRedirect(fragment)
        if (out is AuthOutcome.Success) {
            prefs.edit().putBoolean(KEY_GUEST, false).apply()
            refresh()
        }
        return out
    }

    fun loginAsGuest() {
        prefs.edit().putBoolean(KEY_GUEST, true).apply()
        loadGuest()
    }

    suspend fun logout() {
        supabase.signOut()
        prefs.edit().putBoolean(KEY_GUEST, false).apply()
        profile = null
        stats = UserStats()
        recentWalks = emptyList()
        discoveredPoiIds = emptySet()
        favorites = emptyList()
    }

    // ------------------------------------------------------------- CARGA DE DATOS

    /** Descarga perfil, estadísticas, caminatas, POIs descubiertos y favoritos. */
    suspend fun refresh() {
        if (supabase.session == null) {
            if (prefs.getBoolean(KEY_GUEST, false)) loadGuest()
            return
        }
        isSyncing = true
        try {
            val uid = supabase.session?.userId ?: return
            supabase.select("profiles", "select=*&id=eq.$uid")?.optJSONObject(0)?.let { p ->
                profile = UserProfile(
                    id = uid,
                    name = p.optString("name", "Caminante"),
                    email = supabase.session?.email.orEmpty(),
                    runnerType = p.optString("runner_type", "Explorador Urbano"),
                    avatarEmoji = p.optString("avatar_emoji", "👟"),
                    registeredAt = parseIso(p.optString("created_at"))
                )
            }
            if (profile == null && supabase.session != null) {
                profile = UserProfile(uid, "Caminante", supabase.session?.email.orEmpty())
            }
            supabase.select("user_stats", "select=*&user_id=eq.$uid")?.optJSONObject(0)?.let { s ->
                stats = UserStats(
                    totalKm = s.optDouble("total_km", 0.0).toFloat(),
                    totalWalks = s.optInt("total_walks", 0),
                    totalMinutes = (s.optLong("total_seconds", 0) / 60).toInt(),
                    discoveredPois = s.optInt("discovered_pois", 0)
                )
            }
            supabase.select("walks", "select=*&order=started_at.desc&limit=20")?.let { arr ->
                recentWalks = (0 until arr.length()).map { i ->
                    val w = arr.getJSONObject(i)
                    WalkRecord(
                        startedAt = parseIso(w.optString("started_at")),
                        distanceMeters = w.optDouble("distance_m", 0.0),
                        durationSeconds = w.optInt("duration_s", 0),
                        avgSpeedKmh = w.optDouble("avg_speed_kmh", 0.0).toFloat(),
                        avgCadence = w.optInt("avg_cadence_spm", 0),
                        destinationName = w.optString("destination_name").takeIf { it.isNotBlank() && it != "null" }
                    )
                }
            }
            supabase.select("discovered_pois", "select=poi_id")?.let { arr ->
                discoveredPoiIds = (0 until arr.length()).map { arr.getJSONObject(it).getString("poi_id") }.toSet()
            }
            supabase.select("favorite_places", "select=*&order=created_at.desc")?.let { arr ->
                favorites = (0 until arr.length()).map { i ->
                    val f = arr.getJSONObject(i)
                    FavoritePlace(f.getString("id"), f.getString("name"), f.getDouble("lat"), f.getDouble("lon"))
                }
            }
        } finally {
            isSyncing = false
        }
    }

    suspend fun updateProfile(name: String, runnerType: String, avatarEmoji: String): Boolean {
        val current = profile ?: return false
        profile = current.copy(name = name, runnerType = runnerType, avatarEmoji = avatarEmoji)
        if (isGuest) {
            prefs.edit().putString("guest_name", name).putString("guest_type", runnerType)
                .putString("guest_avatar", avatarEmoji).apply()
            return true
        }
        val patch = JSONObject().put("name", name).put("runner_type", runnerType)
            .put("avatar_emoji", avatarEmoji).put("updated_at", isoUtc(System.currentTimeMillis()))
        return supabase.update("profiles", "id=eq.${current.id}", patch)
    }

    // ------------------------------------------------------------- ESCRITURA

    suspend fun saveWalk(walk: WalkRecord, endedAtMs: Long, routeId: String?): Boolean {
        // Actualización optimista de la UI
        recentWalks = (listOf(walk) + recentWalks).take(20)
        stats = stats.copy(
            totalKm = stats.totalKm + (walk.distanceMeters / 1000.0).toFloat(),
            totalWalks = stats.totalWalks + 1,
            totalMinutes = stats.totalMinutes + walk.durationSeconds / 60
        )
        if (isGuest) {
            saveGuestStats()
            return true
        }
        val row = JSONObject()
            .put("started_at", isoUtc(walk.startedAt))
            .put("ended_at", isoUtc(endedAtMs))
            .put("distance_m", walk.distanceMeters)
            .put("duration_s", walk.durationSeconds)
            .put("avg_speed_kmh", walk.avgSpeedKmh.toDouble())
            .put("avg_cadence_spm", walk.avgCadence)
            .put("destination_name", walk.destinationName ?: JSONObject.NULL)
            .put("route_id", routeId ?: JSONObject.NULL)
        return supabase.insert("walks", row)
    }

    suspend fun markPoiDiscovered(poiId: String) {
        if (poiId in discoveredPoiIds) return
        discoveredPoiIds = discoveredPoiIds + poiId
        stats = stats.copy(discoveredPois = discoveredPoiIds.size)
        if (isGuest) {
            prefs.edit().putStringSet("guest_pois", discoveredPoiIds).apply()
            return
        }
        supabase.insert("discovered_pois", JSONObject().put("poi_id", poiId), upsert = true, onConflict = "user_id,poi_id")
    }

    suspend fun toggleFavorite(name: String, lat: Double, lon: Double) {
        val existing = favorites.find { it.name == name }
        if (existing != null) {
            favorites = favorites - existing
            if (!isGuest) supabase.delete("favorite_places", "id=eq.${existing.id}")
        } else {
            favorites = listOf(FavoritePlace("local-${System.nanoTime()}", name, lat, lon)) + favorites
            if (!isGuest) {
                supabase.insert(
                    "favorite_places",
                    JSONObject().put("name", name).put("lat", lat).put("lon", lon),
                    upsert = true, onConflict = "user_id,name"
                )
                // Recargar para obtener los ids reales del servidor
                supabase.select("favorite_places", "select=*&order=created_at.desc")?.let { arr ->
                    favorites = (0 until arr.length()).map { i ->
                        val f = arr.getJSONObject(i)
                        FavoritePlace(f.getString("id"), f.getString("name"), f.getDouble("lat"), f.getDouble("lon"))
                    }
                }
            }
        }
    }

    suspend fun logTrack(source: String, trackId: String, title: String, artist: String?, bpm: Int?) {
        if (isGuest || supabase.session == null) return
        val row = JSONObject()
            .put("source", source).put("track_id", trackId).put("title", title)
            .put("artist", artist ?: JSONObject.NULL).put("bpm", bpm ?: JSONObject.NULL)
        supabase.insert("music_history", row)
    }

    // ------------------------------------------------------------- INVITADO (LOCAL)

    private fun loadGuest() {
        profile = UserProfile(
            id = "guest",
            name = prefs.getString("guest_name", "Invitado") ?: "Invitado",
            email = "Modo invitado (sin sincronizar)",
            runnerType = prefs.getString("guest_type", "Paseante Casual") ?: "Paseante Casual",
            avatarEmoji = prefs.getString("guest_avatar", "🚶") ?: "🚶",
            registeredAt = prefs.getLong("guest_since", System.currentTimeMillis()).also {
                if (!prefs.contains("guest_since")) prefs.edit().putLong("guest_since", it).apply()
            }
        )
        discoveredPoiIds = prefs.getStringSet("guest_pois", emptySet())?.toSet() ?: emptySet()
        stats = UserStats(
            totalKm = prefs.getFloat("guest_km", 0f),
            totalWalks = prefs.getInt("guest_walks", 0),
            totalMinutes = prefs.getInt("guest_minutes", 0),
            discoveredPois = discoveredPoiIds.size
        )
    }

    private fun saveGuestStats() {
        prefs.edit()
            .putFloat("guest_km", stats.totalKm)
            .putInt("guest_walks", stats.totalWalks)
            .putInt("guest_minutes", stats.totalMinutes)
            .apply()
    }

    /** Supabase devuelve timestamptz en UTC ("2026-11-05T15:24:51.157+00:00"). */
    private fun parseIso(s: String?): Long = runCatching {
        utcFormat("yyyy-MM-dd'T'HH:mm:ss").parse(s!!.take(19))!!.time
    }.getOrDefault(System.currentTimeMillis())

    private fun isoUtc(ms: Long): String = utcFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(Date(ms))

    private fun utcFormat(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    companion object {
        private const val KEY_GUEST = "is_guest"
    }
}
