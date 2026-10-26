package com.example.georhythm.network

import com.example.georhythm.data.PoiCategory
import com.example.georhythm.data.PointOfInterest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class OsrmParseTest {

    private val dest = LatLng(1.2160, -77.2780)
    private val stop = PointOfInterest("h2", "Templo de San Juan", "", LatLng(1.2150, -77.2786), PoiCategory.TEMPLO)

    /** Respuesta de OSRM con dos tramos (inicio → parada → destino). */
    private val twoLegs = """
        {"code":"Ok","routes":[{"distance":220.0,"duration":180.0,
          "geometry":{"type":"LineString","coordinates":[
            [-77.2783,1.2146],[-77.2785,1.2148],[-77.2786,1.2150],[-77.2784,1.2155],[-77.2780,1.2160]]},
          "legs":[
            {"steps":[
              {"distance":60,"name":"Calle 19","maneuver":{"type":"depart","modifier":"","location":[-77.2783,1.2146]}},
              {"distance":0,"name":"","maneuver":{"type":"arrive","modifier":"","location":[-77.2786,1.2150]}}]},
            {"steps":[
              {"distance":0,"name":"","maneuver":{"type":"depart","modifier":"","location":[-77.2786,1.2150]}},
              {"distance":160,"name":"Carrera 25","maneuver":{"type":"turn","modifier":"right","location":[-77.2784,1.2155]}},
              {"distance":0,"name":"","maneuver":{"type":"arrive","modifier":"","location":[-77.2780,1.2160]}}]}
          ]}]}
    """.trimIndent()

    private fun parse(json: String) = OsrmService.parse(JSONObject(json), dest, "Catedral", listOf(stop), "r1")

    @Test
    fun `convierte la geometria en puntos lat-lon`() {
        val r = parse(twoLegs)!!
        assertEquals(5, r.points.size)
        assertEquals(1.2146, r.points.first().latitude, 1e-9)
        assertEquals(-77.2783, r.points.first().longitude, 1e-9)
    }

    @Test
    fun `une los pasos de todos los tramos sin repetir salidas`() {
        val steps = parse(twoLegs)!!.steps.map { it.instruction }
        assertEquals(
            listOf(
                "Comienza a caminar por Calle 19",
                "Llegas a Templo de San Juan",
                "Gira a la derecha por Carrera 25",
                "Llegaste a Catedral"
            ),
            steps
        )
    }

    @Test
    fun `cada paso apunta al punto correcto de la polilinea`() {
        val idx = parse(twoLegs)!!.steps.map { it.pointIndex }
        assertEquals(listOf(0, 2, 3, 4), idx)
    }

    @Test
    fun `la duracion es la estimacion peatonal propia a 4_8 km por hora`() {
        val r = parse(twoLegs)!!
        assertEquals(220.0, r.totalDistanceMeters, 0.0)
        assertEquals(220.0 / (4.8 / 3.6), r.totalDurationSeconds, 0.01)
    }

    @Test
    fun `conserva paradas y id de ruta`() {
        val r = parse(twoLegs)!!
        assertEquals("r1", r.routeId)
        assertEquals(listOf("h2"), r.waypoints.map { it.id })
        assertNotNull(r.destinationLatLng)
    }

    @Test
    fun `una respuesta sin ruta devuelve null`() {
        assertNull(parse("""{"code":"NoRoute","message":"Impossible route between points","routes":[]}"""))
    }

    @Test
    fun `los indices de los pasos nunca retroceden`() {
        val idx = parse(twoLegs)!!.steps.map { it.pointIndex }
        assertTrue(idx.zipWithNext().all { (a, b) -> b >= a })
    }
}
