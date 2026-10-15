package com.example.georhythm.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.georhythm.BuildConfig
import com.example.georhythm.data.SpotifyPlaylist
import com.example.georhythm.data.SpotifyTrack
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/** Resultado de pedir reproducción en Spotify. */
enum class SpotifyPlayResult { PLAYING, NO_DEVICE, PREMIUM_REQUIRED, NOT_CONNECTED, ERROR }

data class SpotifyNowPlaying(
    val isPlaying: Boolean,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val progressMs: Long,
    val durationMs: Long
)

/**
 * Spotify Web API con Authorization Code + PKCE (el flujo implícito `response_type=token`
 * fue retirado por Spotify en 2025, por eso la conexión anterior dejó de funcionar).
 */
class SpotifyService(context: Context) {

    private val prefs = context.getSharedPreferences("georhythm_spotify", Context.MODE_PRIVATE)
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val mutex = Mutex()

    val isConnected: Boolean get() = prefs.getString("refresh_token", null) != null

    // ------------------------------------------------------------- AUTORIZACIÓN

    /** Crea la URL de login y guarda el code_verifier para el intercambio posterior. */
    fun buildAuthorizeUri(): Uri {
        val verifier = randomUrlSafe(64)
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        val state = randomUrlSafe(16)
        // Se guardan los últimos intentos: si el usuario aprueba una pestaña anterior de Chrome,
        // su respuesta (con otro state) también se acepta.
        val pending = runCatching { JSONObject(prefs.getString("pkce_pending", "{}")!!) }.getOrDefault(JSONObject())
        pending.put(state, verifier)
        while (pending.length() > 5) pending.remove(pending.keys().next())
        prefs.edit().putString("pkce_pending", pending.toString()).apply()
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", SCOPES)
            .build()
    }

    /** Procesa georhythm://callback?code=...&state=... Devuelve null si todo fue bien, o el error. */
    suspend fun handleRedirect(uri: Uri): String? {
        val result = handleRedirectInternal(uri)
        android.util.Log.w("GeoSpotify", "redirect keys=${uri.queryParameterNames} error=${uri.getQueryParameter("error")} -> ${result ?: "OK"}")
        return result
    }

    private suspend fun handleRedirectInternal(uri: Uri): String? {
        uri.getQueryParameter("error")?.let {
            return if (it == "access_denied") "Cancelaste el permiso en Spotify." else "Spotify: $it"
        }
        val code = uri.getQueryParameter("code") ?: return "Respuesta de Spotify sin código."
        val state = uri.getQueryParameter("state")
        val pending = runCatching { JSONObject(prefs.getString("pkce_pending", "{}")!!) }.getOrDefault(JSONObject())
        val verifier = state?.let { pending.optString(it).takeIf { v -> v.isNotBlank() } }
            ?: return "Esa ventana de Spotify era de un intento anterior. Toca \"Conectar con Spotify\" otra vez."
        val res = tokenRequest(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to REDIRECT_URI,
            "client_id" to clientId,
            "code_verifier" to verifier
        )
        if (!res.ok) {
            android.util.Log.w("GeoSpotify", "token exchange HTTP ${res.code}: ${res.body.take(300)}")
            val err = runCatching { JSONObject(res.body).optString("error_description") }.getOrNull()
            return if (res.code == -1) "Sin conexión a internet." else "No se pudo conectar Spotify: ${err ?: res.code}"
        }
        storeTokens(JSONObject(res.body))
        prefs.edit().remove("pkce_pending").remove("pkce_verifier").remove("pkce_state").apply()
        return null
    }

    fun disconnect() {
        prefs.edit().clear().apply()
    }

    private suspend fun accessToken(): String? = mutex.withLock {
        val token = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0)
        if (token != null && System.currentTimeMillis() < expiresAt - 60_000) return token
        val refresh = prefs.getString("refresh_token", null) ?: return null
        val res = tokenRequest("grant_type" to "refresh_token", "refresh_token" to refresh, "client_id" to clientId)
        return when {
            res.ok -> storeTokens(JSONObject(res.body))
            res.code == 400 -> { disconnect(); null } // refresh token revocado
            else -> null
        }
    }

    private suspend fun tokenRequest(vararg params: Pair<String, String>): HttpResult {
        val form = params.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        return Http.request(
            "https://accounts.spotify.com/api/token", "POST",
            mapOf("Content-Type" to "application/x-www-form-urlencoded"), form
        )
    }

    private fun storeTokens(json: JSONObject): String {
        val access = json.getString("access_token")
        val e = prefs.edit()
            .putString("access_token", access)
            .putLong("expires_at", System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000)
        json.optString("refresh_token").takeIf { it.isNotBlank() }?.let { e.putString("refresh_token", it) }
        e.apply()
        return access
    }

    // ------------------------------------------------------------- WEB API

    private suspend fun api(path: String, method: String = "GET", body: String? = null): HttpResult {
        val token = accessToken() ?: return HttpResult(401, "")
        return Http.request(
            "https://api.spotify.com/v1$path", method,
            mapOf("Authorization" to "Bearer $token"), body ?: if (method == "PUT" || method == "POST") "" else null
        )
    }

    suspend fun nowPlaying(): SpotifyNowPlaying? {
        val res = api("/me/player/currently-playing")
        if (res.code != 200 || res.body.isBlank()) return null
        val json = JSONObject(res.body)
        val item = json.optJSONObject("item") ?: return null
        return SpotifyNowPlaying(
            isPlaying = json.optBoolean("is_playing"),
            title = item.optString("name"),
            artist = joinArtists(item.optJSONArray("artists")),
            imageUrl = firstImage(item.optJSONObject("album")?.optJSONArray("images")),
            progressMs = json.optLong("progress_ms"),
            durationMs = item.optLong("duration_ms", 1)
        )
    }

    suspend fun playlists(): List<SpotifyPlaylist> {
        val res = api("/me/playlists?limit=50")
        if (!res.ok) return emptyList()
        val items = JSONObject(res.body).optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            val p = items.optJSONObject(i) ?: return@mapNotNull null
            SpotifyPlaylist(
                name = p.optString("name"),
                uri = p.optString("uri"),
                imageUrl = firstImage(p.optJSONArray("images")),
                tracksCount = p.optJSONObject("tracks")?.optInt("total")
                    ?: p.optJSONObject("items")?.optInt("total") ?: 0
            )
        }
    }

    suspend fun searchTracks(query: String): List<SpotifyTrack> {
        val q = URLEncoder.encode(query, "UTF-8")
        val res = api("/search?type=track&limit=25&q=$q")
        if (!res.ok) return emptyList()
        val items = JSONObject(res.body).optJSONObject("tracks")?.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            val t = items.optJSONObject(i) ?: return@mapNotNull null
            SpotifyTrack(
                id = t.optString("id"),
                uri = t.optString("uri"),
                title = t.optString("name"),
                artist = joinArtists(t.optJSONArray("artists")),
                imageUrl = firstImage(t.optJSONObject("album")?.optJSONArray("images")),
                durationMs = t.optLong("duration_ms")
            )
        }
    }

    /** Primera playlist pública que coincide con [query] (para el modo ritmo con Spotify). */
    suspend fun findPlaylistUri(query: String): String? {
        val q = URLEncoder.encode(query, "UTF-8")
        val res = api("/search?type=playlist&limit=5&q=$q")
        if (!res.ok) return null
        val items = JSONObject(res.body).optJSONObject("playlists")?.optJSONArray("items") ?: return null
        for (i in 0 until items.length()) {
            val uri = items.optJSONObject(i)?.optString("uri")
            if (!uri.isNullOrBlank()) return uri
        }
        return null
    }

    /** Reproduce pistas ([trackUris]) o un contexto (playlist/álbum) en el dispositivo activo. */
    suspend fun play(trackUris: List<String> = emptyList(), contextUri: String? = null): SpotifyPlayResult {
        if (!isConnected) return SpotifyPlayResult.NOT_CONNECTED
        val body = JSONObject().apply {
            if (contextUri != null) put("context_uri", contextUri)
            if (trackUris.isNotEmpty()) put("uris", JSONArray(trackUris))
        }.toString()
        var res = api("/me/player/play", "PUT", body)
        if (res.code == 404) {
            // No hay dispositivo activo: intentar con el primer dispositivo disponible (p. ej. este teléfono)
            val deviceId = firstDeviceId() ?: return SpotifyPlayResult.NO_DEVICE
            res = api("/me/player/play?device_id=$deviceId", "PUT", body)
        }
        return when {
            res.ok -> SpotifyPlayResult.PLAYING
            res.code == 403 -> SpotifyPlayResult.PREMIUM_REQUIRED
            res.code == 404 -> SpotifyPlayResult.NO_DEVICE
            res.code == 401 -> SpotifyPlayResult.NOT_CONNECTED
            else -> SpotifyPlayResult.ERROR
        }
    }

    suspend fun pause(): Boolean = api("/me/player/pause", "PUT").ok
    suspend fun resume(): Boolean = api("/me/player/play", "PUT").ok
    suspend fun next(): Boolean = api("/me/player/next", "POST").ok
    suspend fun previous(): Boolean = api("/me/player/previous", "POST").ok

    private suspend fun firstDeviceId(): String? {
        val res = api("/me/player/devices")
        if (!res.ok) return null
        val devices = JSONObject(res.body).optJSONArray("devices") ?: return null
        var fallback: String? = null
        for (i in 0 until devices.length()) {
            val d = devices.getJSONObject(i)
            if (d.optBoolean("is_restricted")) continue
            val id = d.optString("id").takeIf { it.isNotBlank() } ?: continue
            if (d.optString("type").equals("Smartphone", true)) return id
            if (fallback == null) fallback = id
        }
        return fallback
    }

    private fun joinArtists(arr: JSONArray?): String =
        if (arr == null) "" else (0 until arr.length()).joinToString(", ") { arr.getJSONObject(it).optString("name") }

    private fun firstImage(arr: JSONArray?): String? =
        if (arr != null && arr.length() > 0) arr.optJSONObject(0)?.optString("url") else null

    private fun randomUrlSafe(bytes: Int): String {
        val b = ByteArray(bytes)
        SecureRandom().nextBytes(b)
        return base64Url(b)
    }

    private fun base64Url(b: ByteArray): String =
        Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    companion object {
        const val REDIRECT_URI = "georhythm://callback"
        private const val SCOPES = "user-read-playback-state user-modify-playback-state " +
            "user-read-currently-playing playlist-read-private playlist-read-collaborative"
    }
}
