package com.example.georhythm.network

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiusParseTest {

    private fun track(
        id: String,
        bpm: Any? = 120.4,
        streamable: Boolean = true,
        gated: Boolean = false,
        canStream: Boolean = true,
        artwork: String? = """{"150x150":"https://cdn/a150.jpg","480x480":"https://cdn/a480.jpg"}"""
    ) = """
        {"id":"$id","title":"Tema $id","duration":200,"genre":"Electronic",
         "bpm":${bpm ?: "null"},"is_streamable":$streamable,"is_stream_gated":$gated,
         "access":{"stream":$canStream},"user":{"name":"Artista $id"},
         "artwork":${artwork ?: "null"}}
    """.trimIndent()

    private fun parse(vararg tracks: String) = AudiusService.parse(JSONArray("[" + tracks.joinToString(",") + "]"))

    @Test
    fun `lee titulo, artista, duracion, genero y BPM redondeado`() {
        val t = parse(track("a")).single()
        assertEquals("Tema a", t.title)
        assertEquals("Artista a", t.artist)
        assertEquals(200, t.durationSeconds)
        assertEquals("Electronic", t.genre)
        assertEquals(120, t.bpm)
    }

    @Test
    fun `prefiere la caratula de 480 px`() {
        assertEquals("https://cdn/a480.jpg", parse(track("a")).single().artworkUrl)
    }

    @Test
    fun `sin caratula queda en null`() {
        assertNull(parse(track("a", artwork = null)).single().artworkUrl)
    }

    @Test
    fun `descarta pistas que no se pueden reproducir`() {
        val ids = parse(
            track("ok"),
            track("no-stream", streamable = false),
            track("pago", gated = true),
            track("sin-acceso", canStream = false)
        ).map { it.id }
        assertEquals(listOf("ok"), ids)
    }

    @Test
    fun `BPM fuera de rango o ausente queda en null`() {
        val r = parse(track("lento", bpm = 20), track("rapido", bpm = 400), track("sin", bpm = null))
        assertTrue(r.all { it.bpm == null })
    }

    @Test
    fun `la URL de streaming incluye el nombre de la app`() {
        val url = parse(track("abc")).single().streamUrl
        assertEquals("https://api.audius.co/v1/tracks/abc/stream?app_name=GeoRhythm", url)
    }
}
