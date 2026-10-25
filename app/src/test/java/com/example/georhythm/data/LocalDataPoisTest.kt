package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataPoisTest {

    // Perímetro urbano de Pasto (mismo viewbox que usa el buscador)
    private val latRange = 1.17..1.25
    private val lonRange = -77.33..-77.24

    @Test
    fun `los ids de los lugares son unicos`() {
        val ids = LocalData.pois.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `los lugares de Pasto estan dentro del perimetro urbano`() {
        LocalData.pois.filterNot { it.id.startsWith("x") }.forEach { poi ->
            assertTrue("${poi.name} fuera de Pasto: ${poi.latLng}",
                poi.latLng.latitude in latRange && poi.latLng.longitude in lonRange)
        }
    }

    @Test
    fun `los lugares fuera de Pasto estan en Narino`() {
        LocalData.pois.filter { it.id.startsWith("x") }.forEach { poi ->
            assertTrue("${poi.name}: ${poi.latLng}",
                poi.latLng.latitude in 0.5..1.9 && poi.latLng.longitude in -78.5..-76.5)
        }
    }

    @Test
    fun `todos los lugares tienen nombre y descripcion`() {
        LocalData.pois.forEach {
            assertTrue("sin nombre: ${it.id}", it.name.isNotBlank())
            assertTrue("sin descripción: ${it.id}", it.description.isNotBlank())
        }
    }

    @Test
    fun `poi devuelve null para un id desconocido`() {
        assertEquals(null, LocalData.poi("no-existe"))
        assertEquals("Plaza de Nariño", LocalData.poi("h1")?.name)
    }
}
