package com.example.georhythm.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StringFormatterDurationTest {

    @Test
    fun `formatea duraciones menores a una hora en MM SS`() {
        assertEquals("05:30", StringFormatter.formatDuration(330))
        assertEquals("00:45", StringFormatter.formatDuration(45))
    }

    @Test
    fun `formatea duraciones mayores a una hora en HH MM SS`() {
        assertEquals("01:15:30", StringFormatter.formatDuration(4530))
    }

    @Test
    fun `formatea ritmo por kilometro con apostrofe y comillas`() {
        assertEquals("5'30\"/km", StringFormatter.formatPace(5.5))
    }
}
