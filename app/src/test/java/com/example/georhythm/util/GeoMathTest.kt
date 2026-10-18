package com.example.georhythm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun `misma coordenada da distancia cero`() {
        assertEquals(0.0, GeoMath.distanceMeters(1.214611, -77.27828, 1.214611, -77.27828), 1e-9)
    }

    @Test
    fun `la distancia es simetrica`() {
        val ab = GeoMath.distanceMeters(1.214611, -77.27828, 1.216309, -77.28848)
        val ba = GeoMath.distanceMeters(1.216309, -77.28848, 1.214611, -77.27828)
        assertEquals(ab, ba, 1e-6)
    }

    @Test
    fun `Plaza de Narino a Unicentro esta a unos 1150 metros en linea recta`() {
        val d = GeoMath.distanceMeters(1.214611, -77.27828, 1.216309, -77.28848)
        assertEquals(1150.0, d, 30.0)
    }

    @Test
    fun `un grado de latitud mide cerca de 111 km`() {
        val d = GeoMath.distanceMeters(0.0, 0.0, 1.0, 0.0)
        assertEquals(111_195.0, d, 50.0)
    }

    @Test
    fun `Pasto a Las Lajas supera los 50 km`() {
        val d = GeoMath.distanceMeters(1.214611, -77.27828, 0.80530, -77.58610)
        assertTrue("distancia = $d", d > 50_000)
    }
}
