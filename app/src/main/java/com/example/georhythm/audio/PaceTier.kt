package com.example.georhythm.audio

/** Nivel de ritmo según la cadencia de pasos. */
enum class PaceTier(val label: String, val minSpm: Int, val spotifyQuery: String) {
    STOPPED("Detenido", 0, "chill acoustic"),
    STROLL("Paseo", 60, "caminata relajada"),
    WALK("Ritmo", 108, "walking 120 bpm"),
    BRISK("Enérgico", 124, "power walk 130 bpm"),
    JOG("Trote", 145, "running 160 bpm");

    companion object {
        fun from(spm: Int): PaceTier = entries.last { spm >= it.minSpm }
    }
}
