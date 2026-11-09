package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncQueueSerializationTest {

    @Test
    fun `evento almacena payload JSON sin alteraciones`() {
        val payload = "{\"route_id\":\"centro\",\"cadence\":120}"
        val event = SyncEvent("uuid-123", SyncEventType.WALK_SESSION, payload)
        assertEquals(payload, event.payloadJson)
    }
}
