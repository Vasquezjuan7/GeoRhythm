package com.example.georhythm.data

import com.example.georhythm.util.GeoMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataRoutesTest {

    private fun km(a: PointOfInterest, b: PointOfInterest) =
        GeoMath.distanceMeters(a.latLng.latitude, a.latLng.longitude, b.latLng.latitude, b.latLng.longitude) / 1000

    @Test
    fun `los ids de las rutas son unicos`() {
        val ids = LocalData.historicalRoutes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `todas las paradas de cada ruta existen`() {
        LocalData.historicalRoutes.forEach { r ->
            assertEquals("ruta ${r.id}", r.stopIds.size, LocalData.stopsOf(r).size)
        }
    }

    @Test
    fun `cada ruta tiene al menos 4 paradas y no repite paradas`() {
        LocalData.historicalRoutes.forEach { r ->
            assertTrue("ruta ${r.id}", r.stopIds.size >= 4)
            assertEquals("ruta ${r.id} repite paradas", r.stopIds.size, r.stopIds.toSet().size)
        }
    }

    @Test
    fun `las rutas recomendadas son solo dentro de Pasto`() {
        LocalData.historicalRoutes.forEach { r ->
            assertTrue("ruta ${r.id} usa lugares fuera de Pasto", r.stopIds.none { it.startsWith("x") })
        }
    }

    @Test
    fun `ningun tramo entre paradas supera 1_5 km`() {
        LocalData.historicalRoutes.forEach { r ->
            LocalData.stopsOf(r).zipWithNext().forEach { (a, b) ->
                assertTrue("ruta ${r.id}: ${a.name} -> ${b.name} = ${km(a, b)} km", km(a, b) < 1.5)
            }
        }
    }

    @Test
    fun `cada ruta completa se puede caminar en menos de 5 km`() {
        LocalData.historicalRoutes.forEach { r ->
            val total = LocalData.stopsOf(r).zipWithNext().sumOf { (a, b) -> km(a, b) }
            assertTrue("ruta ${r.id} = $total km", total < 5.0)
        }
    }

    @Test
    fun `cada ruta tiene al menos una parada con audio-guia`() {
        LocalData.historicalRoutes.forEach { r ->
            assertTrue("ruta ${r.id}", LocalData.stopsOf(r).any { it.story != null })
        }
    }
}
