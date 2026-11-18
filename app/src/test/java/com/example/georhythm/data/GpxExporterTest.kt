package com.example.georhythm.data

import org.junit.Assert.assertTrue
import org.junit.Test

class GpxExporterTest {

    @Test
    fun `genera XML GPX con cabecera y etiquetas requeridas`() {
        val track = GpxTrack(
            name = "Ruta Centro Histórico Pasto",
            description = "Paseo patrimonial con música a 115 BPM",
            points = listOf(
                GpxPoint(1.2136, -77.2811, 2527.0, "2026-11-18T10:00:00Z", 115, 4.5f),
                GpxPoint(1.2140, -77.2805, 2529.0, "2026-11-18T10:01:00Z", 118, 4.7f)
            )
        )
        val xml = GpxExporter.toXml(track)
        assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(xml.contains("<gpx version=\"1.1\""))
        assertTrue(xml.contains("<name>Ruta Centro Histórico Pasto</name>"))
        assertTrue(xml.contains("<trkpt lat=\"1.213600\" lon=\"-77.281100\">"))
        assertTrue(xml.contains("<ele>2527.0</ele>"))
        assertTrue(xml.contains("<georhythm:cadence>115</georhythm:cadence>"))
    }
}
