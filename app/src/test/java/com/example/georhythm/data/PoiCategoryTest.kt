package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PoiCategoryTest {

    @Test
    fun `todas las categorias tienen un nombre legible para la interfaz`() {
        for (cat in PoiCategory.entries) {
            assertNotNull(cat.label)
            assertEquals(false, cat.label.isBlank())
        }
    }
}
