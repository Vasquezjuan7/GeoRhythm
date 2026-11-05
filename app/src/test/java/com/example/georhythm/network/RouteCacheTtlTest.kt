package com.example.georhythm.network

import com.example.georhythm.data.RouteGeometry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RouteCacheTtlTest {

    @Test
    fun `entradas que superan el TTL son eliminadas`() {
        val cache = RouteCache(maxEntries = 10, ttlMs = 1000L)
        val key = RouteCacheKey.fromCoords(1.0, -77.0, 1.1, -77.1)
        val geom = RouteGeometry(emptyList(), 0.0, 0.0)

        cache.put(key, geom, nowMs = 1000L)
        assertNotNull(cache.get(key, nowMs = 1500L))
        assertNull(cache.get(key, nowMs = 2500L))
    }
}
