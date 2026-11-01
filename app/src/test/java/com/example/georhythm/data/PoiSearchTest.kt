package com.example.georhythm.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiSearchTest {

    @Test
    fun `normalizacion elimina tildes y pasa a minusculas`() {
        assertTrue(PoiSearch.matches("narino", "Plaza de Nariño"))
        assertTrue(PoiSearch.matches("san juan", "Templo de San Juan Bautista"))
        assertTrue(PoiSearch.matches("concepcion", "Plazoleta de La Concepción"))
    }

    @Test
    fun `rechaza busquedas sin coincidencia`() {
        assertFalse(PoiSearch.matches("bogota", "Catedral de Pasto"))
    }
}
