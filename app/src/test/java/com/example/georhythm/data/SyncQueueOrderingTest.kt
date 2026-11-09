package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncQueueOrderingTest {

    @Test
    fun `eventos encolados se procesan en orden FIFO estricto`() {
        val q = SyncQueue()
        val e1 = SyncEvent("1", SyncEventType.WALK_SESSION, "{}")
        val e2 = SyncEvent("2", SyncEventType.POI_DISCOVERY, "{}")
        q.enqueue(e1)
        q.enqueue(e2)

        assertEquals("1", q.dequeue()?.id)
        assertEquals("2", q.dequeue()?.id)
        assertEquals(0, q.size())
    }
}
