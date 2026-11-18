package com.example.georhythm.data

import org.junit.Assert.assertTrue
import org.junit.Test

class GpxWaypointTest {

    @Test
    fun `exporta waypoints con nombres de paradas patrimoniales`() {
        val track = GpxTrack(
            name = "Santuarios de Pasto",
            description = "Ruta de templos coloniales",
            waypoints = listOf(
                GpxWaypoint(1.2144, -77.2783, "Templo de San Juan Bautista", "Monumento nacional"),
                GpxWaypoint(1.2115, -77.2798, "Iglesia de San Felipe Neri", "Estilo barroco nariñense")
            )
        )
        val xml = GpxExporter.toXml(track)
        assertTrue(xml.contains("<wpt lat=\"1.214400\" lon=\"-77.278300\">"))
        assertTrue(xml.contains("<name>Templo de San Juan Bautista</name>"))
        assertTrue(xml.contains("<desc>Monumento nacional</desc>"))
        assertTrue(xml.contains("Iglesia de San Felipe Neri"))
    }
}
