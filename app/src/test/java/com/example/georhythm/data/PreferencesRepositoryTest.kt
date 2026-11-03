package com.example.georhythm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesRepositoryTest {

    @Test
    fun `inicia con valores predeterminados y permite actualizacion`() {
        val repo = PreferencesRepository()
        assertEquals(UnitSystem.METRIC, repo.getPreferences().unitSystem)
        assertTrue(repo.getPreferences().voiceGuidanceEnabled)

        repo.updatePreferences { it.copy(voiceGuidanceEnabled = false, unitSystem = UnitSystem.IMPERIAL) }
        assertFalse(repo.getPreferences().voiceGuidanceEnabled)
        assertEquals(UnitSystem.IMPERIAL, repo.getPreferences().unitSystem)

        repo.resetToDefaults()
        assertTrue(repo.getPreferences().voiceGuidanceEnabled)
    }
}
