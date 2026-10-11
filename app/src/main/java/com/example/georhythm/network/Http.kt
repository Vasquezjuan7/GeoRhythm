package com.example.georhythm.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Respuesta HTTP simple (código + cuerpo). */
data class HttpResult(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299
}

/** Cliente HTTP mínimo basado en HttpURLConnection (siempre en Dispatchers.IO). */
object Http {
    const val USER_AGENT = "GeoRhythm/2.0 (Android)"

    suspend fun request(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        timeoutMs: Int = 10_000
    ): HttpResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null) {
                    doOutput = true
                    if (headers.keys.none { it.equals("Content-Type", true) }) {
                        setRequestProperty("Content-Type", "application/json")
                    }
                }
            }
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..399) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            HttpResult(code, text)
        } catch (e: Exception) {
            HttpResult(-1, e.message ?: "Error de red")
        } finally {
            conn?.disconnect()
        }
    }
}
