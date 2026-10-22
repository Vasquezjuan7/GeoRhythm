package com.example.georhythm.data

import com.example.georhythm.data.RouteFixtures.fixAt
import com.example.georhythm.data.RouteFixtures.stop
import com.example.georhythm.data.RouteFixtures.straightRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkTrackerStopsTest {

    private var now = 0L
    private val tracker = WalkTracker { now }

    private fun walkTo(index: Int): List<WalkEvent> {
        now += 8_000
        return tracker.onFix(fixAt(index, now), emptySet())
    }

    @Test
    fun `las paradas se alcanzan en orden`() {
        tracker.startRoute(straightRoute(waypoints = listOf(stop("a", 10), stop("b", 25), stop("c", 39))))
        walkTo(0)
        val reached = walkTo(10).filterIsInstance<WalkEvent.ReachedStop>().single()
        assertEquals("a", reached.poi.id)
        assertEquals(0, reached.index)
        assertEquals(3, reached.total)
        assertEquals(1, tracker.nextStopIndex)
    }

    @Test
    fun `no se llega al destino sin pasar por todas las paradas`() {
        tracker.startRoute(straightRoute(waypoints = listOf(stop("a", 10), stop("c", 39))))
        walkTo(0)
        // salta directo al final sin pasar por la parada "a"
        assertTrue(walkTo(39).none { it is WalkEvent.Arrived })
    }

    @Test
    fun `al llegar al destino emite Arrived con el registro de la caminata`() {
        tracker.startRoute(straightRoute())
        for (i in 0..39 step 3) walkTo(i)
        val arrived = walkTo(39).filterIsInstance<WalkEvent.Arrived>().single()
        assertEquals("test", arrived.routeId)
        assertTrue("distancia = ${arrived.record.distanceMeters}", arrived.record.distanceMeters > 350)
    }

    @Test
    fun `remainingStops devuelve solo las paradas que faltan`() {
        tracker.startRoute(straightRoute(waypoints = listOf(stop("a", 10), stop("b", 25), stop("c", 39))))
        walkTo(0)
        walkTo(10)
        assertEquals(listOf("b", "c"), tracker.remainingStops().map { it.id })
    }
}
