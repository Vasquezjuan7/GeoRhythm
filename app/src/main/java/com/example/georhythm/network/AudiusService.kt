package com.example.georhythm.network

import com.example.georhythm.data.AudiusTrack
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Catálogo musical abierto de Audius (https://docs.audius.org).
 * Permite buscar y reproducir canciones completas de forma legal y entrega el BPM real de cada pista,
 * lo que hace posible sincronizar la música con la cadencia de los pasos.
 */
object AudiusService {
    private const val BASE = "https://api.audius.co/v1"
    private const val APP = "app_name=GeoRhythm"

    suspend fun search(query: String, limit: Int = 40): List<AudiusTrack> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        return fetch("$BASE/tracks/search?query=$q&limit=$limit&$APP")
    }

    suspend fun trending(genre: String? = null, limit: Int = 50): List<AudiusTrack> {
        val g = genre?.let { "&genre=" + URLEncoder.encode(it, "UTF-8") } ?: ""
        return fetch("$BASE/tracks/trending?time=week&limit=$limit$g&$APP")
    }

    /**
     * Construye una bolsa variada de canciones con BPM conocido para el modo "Ritmo al paso".
     */
    suspend fun bpmPool(): List<AudiusTrack> = coroutineScope {
        listOf(null, "Electronic", "Latin", "Hip-Hop/Rap", "Pop", "Alternative", "Lo-Fi")
            .map { g -> async { trending(g, 60) } }
            .awaitAll()
            .flatten()
            .filter { it.bpm != null }
            .distinctBy { it.id }
    }

    private suspend fun fetch(url: String): List<AudiusTrack> {
        val res = Http.request(url, timeoutMs = 12_000)
        if (!res.ok) return emptyList()
        val data = runCatching { JSONObject(res.body).optJSONArray("data") }.getOrNull() ?: return emptyList()
        return parse(data)
    }

    private fun parse(arr: JSONArray): List<AudiusTrack> {
        val out = ArrayList<AudiusTrack>(arr.length())
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i) ?: continue
            if (!t.optBoolean("is_streamable", true)) continue
            if (t.optBoolean("is_stream_gated", false)) continue
            if (t.optJSONObject("access")?.optBoolean("stream", true) == false) continue
            val id = t.optString("id")
            if (id.isBlank()) continue
            val art = t.optJSONObject("artwork")
            val rawBpm = t.optDouble("bpm", Double.NaN)
            out += AudiusTrack(
                id = id,
                title = t.optString("title", "Sin título"),
                artist = t.optJSONObject("user")?.optString("name").orEmpty().ifBlank { "Audius" },
                durationSeconds = t.optInt("duration", 0),
                artworkUrl = art?.optString("480x480")?.takeIf { it.startsWith("http") }
                    ?: art?.optString("150x150")?.takeIf { it.startsWith("http") },
                genre = t.optString("genre", ""),
                bpm = if (rawBpm.isNaN() || rawBpm < 50 || rawBpm > 220) null else rawBpm.roundToInt()
            )
        }
        return out
    }
}
