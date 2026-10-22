package com.example.georhythm.data

import com.example.georhythm.data.RouteFixtures.fixAt
import com.example.georhythm.data.RouteFixtures.straightRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkTrackerOffRouteTest {

    private var now = 0L
    private val tracker = WalkTracker { now }
    private val farOff = 0.0008 // ~89 m al oeste de la ruta

    private fun offRouteEvents(index: Int): List<WalkEvent> {
        now += 3_000
        return tracker.onFix(fixAt(index, now, lonOffset = -farOff), emptySet())
    }

    @Test
    fun `una sola lectura lejos no dispara recalculo`() {
        tracker.startRoute(straightRoute())
        assertTrue(offRouteEvents(5).none { it == WalkEvent.OffRoute })
    }

    @Test
    fun `tres lecturas seguidas lejos de la ruta disparan OffRoute`() {
        tracker.startRoute(straightRoute())
        now = 30_000 // el primer recálculo no está limitado
        val all = (1..3).flatMap { offRouteEvents(5) }
        assertEquals(1, all.count { it == WalkEvent.OffRoute })
    }

    @Test
    fun `no recalcula mas de una vez cada 20 segundos`() {
        tracker.startRoute(straightRoute())
        now = 30_000
        val all = (1..6).flatMap { offRouteEvents(5) } // 18 s fuera de la ruta
        assertEquals(1, all.count { it == WalkEvent.OffRoute })
    }

    @Test
    fun `volver a la ruta reinicia el contador`() {
        tracker.startRoute(straightRoute())
        now = 30_000
        offRouteEvents(5)
        offRouteEvents(5)
        now += 3_000
        tracker.onFix(fixAt(6, now), emptySet()) // de vuelta en la ruta
        assertTrue(offRouteEvents(7).none { it == WalkEvent.OffRoute })
    }
}
