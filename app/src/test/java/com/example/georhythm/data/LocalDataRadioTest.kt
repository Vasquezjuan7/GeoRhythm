package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataRadioTest {

    @Test
    fun `las emisoras usan https`() {
        LocalData.radioStations.forEach { assertTrue(it.streamUrl, it.streamUrl.startsWith("https://")) }
    }

    @Test
    fun `los BPM de las emisoras cubren los ritmos de caminata`() {
        val bpms = LocalData.radioStations.map { it.targetBpm }
        assertTrue("mínimo ${bpms.min()}", bpms.min() <= 90)
        assertTrue("máximo ${bpms.max()}", bpms.max() >= 130)
    }

    @Test
    fun `los ids de emisoras y regiones son unicos`() {
        assertEquals(LocalData.radioStations.size, LocalData.radioStations.map { it.id }.toSet().size)
        assertEquals(LocalData.regions.size, LocalData.regions.map { it.id }.toSet().size)
    }

    @Test
    fun `los colores de portada son hexadecimales validos`() {
        val hex = Regex("^#[0-9A-Fa-f]{6}$")
        LocalData.radioStations.forEach {
            assertTrue(it.coverGradientHex.first, hex.matches(it.coverGradientHex.first))
            assertTrue(it.coverGradientHex.second, hex.matches(it.coverGradientHex.second))
        }
        PoiCategory.entries.forEach { assertTrue(it.colorHex, hex.matches(it.colorHex)) }
    }
}
