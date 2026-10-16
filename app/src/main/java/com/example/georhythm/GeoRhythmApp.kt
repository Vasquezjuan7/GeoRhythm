package com.example.georhythm

import android.app.Application
import com.example.georhythm.core.GeoRhythmController
import org.maplibre.android.MapLibre

class GeoRhythmApp : Application() {
    /** Estado de la app a nivel de proceso: sobrevive a rotaciones y a la pantalla apagada. */
    val controller: GeoRhythmController by lazy { GeoRhythmController(this) }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
