package com.example.georhythm.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationGradeTest {

    @Test
    fun `pendiente porcentual se calcula con trigonometria simple`() {
        assertEquals(10.0, ElevationMath.gradePercentage(10.0, 100.0), 1e-4)
        assertEquals(0.0, ElevationMath.gradePercentage(0.0, 100.0), 1e-4)
    }
}
