package com.example.georhythm.data

import com.example.georhythm.data.RouteFixtures.fixAt
import com.example.georhythm.data.RouteFixtures.straightRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkTrackerNavigationTest {

    private var now = 0L
    private val tracker = WalkTracker { now }

    @Test
    fun `la distancia restante disminuye al avanzar`() {
        tracker.startRoute(straightRoute())
        tracker.onFix(fixAt(0, now), emptySet())
        val start = tracker.remainingMeters
        now += 10_000
        tracker.onFix(fixAt(20, now), emptySet())
        assertTrue("inicio=$start ahora=${tracker.remainingMeters}", tracker.remainingMeters < start - 150)
    }

    @Test
    fun `la proxima indicacion es la siguiente maniobra por delante`() {
        tracker.startRoute(straightRoute())
        tracker.onFix(fixAt(3, now), emptySet())
        assertEquals("Gira a la derecha por Calle 18", tracker.nextInstruction?.instruction)
        now += 30_000
        tracker.onFix(fixAt(25, now), emptySet())
        assertEquals("Llegaste a Destino de prueba", tracker.nextInstruction?.instruction)
    }

    @Test
    fun `se ignoran lecturas imprecisas`() {
        tracker.startRoute(straightRoute())
        val events = tracker.onFix(fixAt(10, now).copy(accuracyM = 80f), emptySet())
        assertTrue(events.isEmpty())
        assertEquals(straightRoute().totalDistanceMeters, tracker.remainingMeters, 0.1)
    }

    @Test
    fun `detener la ruta limpia la navegacion`() {
        tracker.startRoute(straightRoute())
        tracker.stopRoute()
        assertNull(tracker.route)
        assertNull(tracker.nextInstruction)
    }
}
