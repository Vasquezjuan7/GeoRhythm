package com.example.georhythm.data

import com.example.georhythm.data.RouteFixtures.fixAt
import com.example.georhythm.data.RouteFixtures.straightRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkTrackerSessionTest {

    private var now = 1_000_000L
    private val tracker = WalkTracker { now }

    @Test
    fun `una caminata de menos de 50 m no se guarda`() {
        tracker.startRoute(straightRoute())
        tracker.onFix(fixAt(0, now), emptySet())
        now += 5_000
        tracker.onFix(fixAt(3, now), emptySet())
        assertNull(tracker.stopRoute())
    }

    @Test
    fun `el registro incluye distancia, duracion y promedios`() {
        tracker.startRoute(straightRoute())
        for (i in 0..30) {
            tracker.onFix(fixAt(i, now), emptySet())
            now += 8_000
        }
        val record = tracker.stopRoute()
        assertNotNull(record)
        record!!
        assertEquals(30 * 11.1, record.distanceMeters, 15.0)
        assertEquals(248, record.durationSeconds)
        assertTrue("cadencia = ${record.avgCadence}", record.avgCadence in 110..130)
        assertEquals("Destino de prueba", record.destinationName)
    }

    @Test
    fun `los saltos de GPS mayores a 60 m no suman distancia`() {
        tracker.startRoute(straightRoute())
        tracker.onFix(fixAt(0, now), emptySet())
        now += 8_000
        tracker.onFix(fixAt(20, now), emptySet()) // salto de ~222 m
        assertEquals(0.0, tracker.walkDistanceMeters, 0.01)
    }

    @Test
    fun `un recalculo conserva la sesion`() {
        tracker.startRoute(straightRoute())
        for (i in 0..10) {
            tracker.onFix(fixAt(i, now), emptySet())
            now += 8_000
        }
        val before = tracker.walkDistanceMeters
        tracker.startRoute(straightRoute(), keepSession = true)
        assertEquals(before, tracker.walkDistanceMeters, 0.0)
        assertNotNull(tracker.walkStartedAt)
    }
}
