package com.example.georhythm.network

import com.example.georhythm.data.NavigationDestination
import org.json.JSONArray
import org.maplibre.android.geometry.LatLng
import java.net.URLEncoder

object NominatimService {

    /**
     * Busca lugares en OpenStreetMap. Prioriza el perímetro urbano de Pasto (viewbox estricto)
     * y si no hay resultados amplía la búsqueda a Nariño.
     * Política de uso de Nominatim: máx. 1 petición/s → la UI aplica un retardo antes de buscar.
     */
    suspend fun searchPlaces(query: String): List<NavigationDestination> {
        val q = query.trim()
        if (q.length < 3) return emptyList()
        val local = request(q, "&viewbox=-77.3300,1.2500,-77.2400,1.1700&bounded=1")
        if (local.isNotEmpty()) return local
        return request("$q, Nariño", "&countrycodes=co")
    }

    private suspend fun request(q: String, extra: String): List<NavigationDestination> {
        val url = "https://nominatim.openstreetmap.org/search?q=${URLEncoder.encode(q, "UTF-8")}" +
            "&format=json&limit=8&accept-language=es$extra"
        val res = Http.request(url, timeoutMs = 8_000)
        if (!res.ok) return emptyList()
        val arr = runCatching { JSONArray(res.body) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            val parts = item.getString("display_name").split(",").map { it.trim() }
            NavigationDestination(
                name = parts.firstOrNull() ?: q,
                category = parts.drop(1).take(3).joinToString(", ").ifBlank { "Nariño, Colombia" },
                latLng = LatLng(item.getDouble("lat"), item.getDouble("lon"))
            )
        }.distinctBy { it.name + it.category }
    }
}
