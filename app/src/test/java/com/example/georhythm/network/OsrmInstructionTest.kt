package com.example.georhythm.network

import org.junit.Assert.assertEquals
import org.junit.Test

class OsrmInstructionTest {

    private fun i(type: String, modifier: String = "", street: String = "") =
        OsrmService.instruction(type, modifier, street, "Plaza de Nariño")

    @Test
    fun `salida y llegada`() {
        assertEquals("Comienza a caminar por Calle 18", i("depart", street = "Calle 18"))
        assertEquals("Llegaste a Plaza de Nariño", i("arrive"))
    }

    @Test
    fun `giros a izquierda y derecha con nombre de calle`() {
        assertEquals("Gira a la izquierda por Carrera 27", i("turn", "left", "Carrera 27"))
        assertEquals("Gira a la derecha por Calle 19", i("turn", "right", "Calle 19"))
    }

    @Test
    fun `giros suaves y cerrados`() {
        assertEquals("Mantente a la izquierda", i("turn", "slight left"))
        assertEquals("Mantente a la derecha", i("turn", "slight right"))
        assertEquals("Gira cerrado a la izquierda", i("turn", "sharp left"))
        assertEquals("Gira cerrado a la derecha", i("turn", "sharp right"))
        assertEquals("Da la vuelta", i("turn", "uturn"))
    }

    @Test
    fun `glorietas`() {
        assertEquals("Toma la glorieta por Avenida Panamericana", i("roundabout", street = "Avenida Panamericana"))
        assertEquals("Toma la glorieta", i("rotary"))
    }

    @Test
    fun `continuar recto o por una calle con otro nombre`() {
        assertEquals("Continúa por Calle 18", i("continue", "straight", "Calle 18"))
        assertEquals("Continúa", i("new name", "straight"))
        assertEquals("Gira a la derecha por Calle 20", i("continue", "right", "Calle 20"))
    }

    @Test
    fun `sin nombre de calle no agrega el por`() {
        assertEquals("Sigue derecho", i("turn", "straight"))
    }
}
