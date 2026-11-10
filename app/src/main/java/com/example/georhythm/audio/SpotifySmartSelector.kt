package com.example.georhythm.audio

class SpotifySmartSelector(
    private val mappings: List<CadencePlaylistMapping> = defaultMappings
) {
    companion object {
        val defaultMappings = listOf(
            CadencePlaylistMapping(0, 80, "Paseo Calmo & Chill", "spotify:playlist:calm"),
            CadencePlaylistMapping(81, 110, "Caminata Urbana", "spotify:playlist:urban"),
            CadencePlaylistMapping(111, 130, "Paso Firme San Juan", "spotify:playlist:brisk"),
            CadencePlaylistMapping(131, 200, "Trote Energético", "spotify:playlist:energy")
        )
    }

    private var currentPlaylist: CadencePlaylistMapping? = null

    fun selectForCadence(cadence: Int): CadencePlaylistMapping {
        val found = mappings.find { cadence in it.minCadence..it.maxCadence }
            ?: mappings.last()
        currentPlaylist = found
        return found
    }
}
