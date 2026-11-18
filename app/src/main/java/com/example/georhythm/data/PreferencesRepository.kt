package com.example.georhythm.data

class PreferencesRepository(
    private var preferences: UserPreferences = UserPreferences()
) {
    fun getPreferences(): UserPreferences = preferences

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences): UserPreferences {
        preferences = transform(preferences)
        return preferences
    }

    fun resetToDefaults(): UserPreferences {
        preferences = UserPreferences()
        return preferences
    }
}
