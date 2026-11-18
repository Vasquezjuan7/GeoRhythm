package com.example.georhythm.data

import org.junit.Assert.assertTrue
import org.junit.Test

class WalkTrackerDiscoveryTest {

    private val plaza = LocalData.poi("h1")!!
    private val tracker = WalkTracker { 0L }

    private fun fixAtPlaza() = GpsFix(plaza.latLng.latitude, plaza.latLng.longitude, accuracyM = 5f)

    @Test
    fun `pasar junto a un lugar lo descubre`() {
        val events = tracker.onFix(fixAtPlaza(), emptySet())
        assertTrue(events.filterIsInstance<WalkEvent.Discovered>().any { it.poi.id == "h1" })
    }

    @Test
    fun `un lugar ya descubierto no se vuelve a anunciar`() {
        val events = tracker.onFix(fixAtPlaza(), setOf("h1"))
        assertTrue(events.filterIsInstance<WalkEvent.Discovered>().none { it.poi.id == "h1" })
    }

    @Test
    fun `lejos de todos los lugares no se descubre nada`() {
        // Punto al occidente de la ciudad, lejos de los lugares registrados
        val events = tracker.onFix(GpsFix(1.2290, -77.3020, accuracyM = 5f), emptySet())
        assertTrue(events.none { it is WalkEvent.Discovered })
    }
}
