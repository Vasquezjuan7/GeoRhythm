package com.example.georhythm.network

import com.example.georhythm.data.RouteGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RouteCacheTest {

    @Test
    fun `almacena y recupera rutas por clave de coordenadas`() {
        val cache = RouteCache(maxEntries = 2)
        val key = RouteCacheKey.fromCoords(1.2136, -77.2811, 1.2140, -77.2805)
        val geom = RouteGeometry(points = listOf(1.2136 to -77.2811, 1.2140 to -77.2805), totalDistanceMeters = 120.0, totalDurationSeconds = 90.0)

        cache.put(key, geom)
        assertNotNull(cache.get(key))
        assertEquals(1, cache.size())
    }

    @Test
    fun `desaloja la entrada mas antigua al superar la capacidad maxima`() {
        val cache = RouteCache(maxEntries = 2)
        val k1 = RouteCacheKey.fromCoords(1.0, -77.0, 1.1, -77.1)
        val k2 = RouteCacheKey.fromCoords(1.2, -77.2, 1.3, -77.3)
        val k3 = RouteCacheKey.fromCoords(1.4, -77.4, 1.5, -77.5)
        val dummy = RouteGeometry(emptyList(), 0.0, 0.0)

        cache.put(k1, dummy)
        cache.put(k2, dummy)
        cache.put(k3, dummy)

        assertNull(cache.get(k1))
        assertNotNull(cache.get(k2))
        assertNotNull(cache.get(k3))
    }
}
