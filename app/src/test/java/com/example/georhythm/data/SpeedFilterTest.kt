package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedFilterTest {

    private fun fix(lat: Double, t: Long, speed: Float? = null) = GpsFix(lat, -77.28, speedMps = speed, timeMs = t)

    @Test
    fun `usa la velocidad del GPS cuando existe`() {
        val f = SpeedFilter()
        assertEquals(5.0f, f.update(fix(1.21, 0, speed = 1.3889f)), 0.05f) // 1.39 m/s = 5 km/h
    }

    @Test
    fun `sin velocidad del GPS la calcula con distancia sobre tiempo`() {
        val f = SpeedFilter(alpha = 1f)
        f.update(fix(1.2100, 0))
        // 0.0001° de latitud ≈ 11.1 m en 8 s ≈ 1.39 m/s ≈ 5 km/h
        assertEquals(5.0f, f.update(fix(1.2101, 8_000)), 0.1f)
    }

    @Test
    fun `por debajo del umbral se considera quieto`() {
        val f = SpeedFilter()
        assertEquals(0f, f.update(fix(1.21, 0, speed = 0.2f)), 0f)
    }

    @Test
    fun `descarta velocidades imposibles para una caminata`() {
        val f = SpeedFilter()
        val kmh = f.update(fix(1.21, 0, speed = 25f)) // 90 km/h: probablemente un bus o un error
        assertTrue("kmh = $kmh", kmh <= 8f * 3.6f + 0.01f)
    }

    @Test
    fun `suaviza un pico aislado`() {
        val f = SpeedFilter()
        f.update(fix(1.21, 0, speed = 1.4f))
        f.update(fix(1.21, 1_000, speed = 1.4f))
        val spike = f.update(fix(1.21, 2_000, speed = 6f))
        assertTrue("pico suavizado = $spike km/h", spike < 6f * 3.6f * 0.6f)
    }

    @Test
    fun `ignora intervalos de tiempo absurdos`() {
        val f = SpeedFilter(alpha = 1f)
        f.update(fix(1.2100, 0))
        assertEquals(0f, f.update(fix(1.2200, 120_000)), 0f) // 2 minutos sin señal
    }
}
