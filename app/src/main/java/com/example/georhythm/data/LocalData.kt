package com.example.georhythm.data

import org.maplibre.android.geometry.LatLng

/**
 * Datos locales. Las coordenadas de Pasto fueron verificadas contra OpenStreetMap (Nominatim)
 * el 2026-11-05.
 */
object LocalData {

    // --- REGIONES PARA EXPLORAR EN EL MAPA ---
    val regions = listOf(
        CityRegion("pasto", "Pasto", LatLng(1.21461, -77.27828), 15.5),
        CityRegion("cocha", "La Cocha", LatLng(1.15500, -77.16300), 13.5),
        CityRegion("galeras", "Galeras", LatLng(1.22000, -77.37000), 12.5),
        CityRegion("lajas", "Las Lajas", LatLng(0.80530, -77.58610), 15.5),
        CityRegion("sandona", "Sandoná", LatLng(1.28694, -77.47222), 15.0),
        CityRegion("aeropuerto", "Aeropuerto", LatLng(1.39625, -77.29150), 14.5)
    )

    // --- LUGARES ---
    val pois: List<PointOfInterest> = listOf(
        // Centro histórico
        PointOfInterest("h1", "Plaza de Nariño", "Calle 19 con Carrera 25", LatLng(1.214611, -77.278280), PoiCategory.HISTORIA,
            "Plaza principal de Pasto, presidida por la estatua de Antonio Nariño.",
            "Estás en la Plaza de Nariño, el corazón cívico de San Juan de Pasto. Desde la época colonial aquí se concentraron el comercio, el gobierno y las celebraciones de la ciudad."),
        PointOfInterest("h2", "Templo de San Juan Bautista", "Calle 18, Las Cuadras", LatLng(1.215140, -77.278650), PoiCategory.TEMPLO,
            "Uno de los templos más antiguos de la ciudad, ligado a su fundación.",
            "Este es el Templo de San Juan Bautista, santo patrono que da nombre a la ciudad. Su historia se remonta a los primeros años de la fundación de Pasto en el siglo dieciséis."),
        PointOfInterest("h3", "Catedral de Pasto", "Plazoleta de la Catedral", LatLng(1.215727, -77.279620), PoiCategory.TEMPLO,
            "Catedral del Sagrado Corazón de Jesús, sede de la Diócesis de Pasto.",
            "Frente a ti está la Catedral del Sagrado Corazón de Jesús, sede de la Diócesis de Pasto y uno de los edificios religiosos más importantes del sur de Colombia."),
        PointOfInterest("h4", "Parroquia de San Agustín", "Calle 17, Las Cuadras", LatLng(1.213226, -77.279410), PoiCategory.TEMPLO,
            "Templo colonial junto a la plazoleta de San Agustín."),
        PointOfInterest("h5", "Teatro Imperial", "Carrera 26, Santiago", LatLng(1.214617, -77.282310), PoiCategory.CULTURA,
            "Teatro histórico del centro, escenario de cine, música y artes escénicas.",
            "Llegaste al Teatro Imperial, un escenario histórico del centro de Pasto donde por generaciones se han presentado cine, música y teatro."),
        PointOfInterest("h6", "Museo Taminango", "Calle 13, San Andresito", LatLng(1.215204, -77.284290), PoiCategory.HISTORIA,
            "Casona colonial restaurada; museo de artes y tradiciones populares.",
            "Este es el Museo Taminango, una de las casonas civiles más antiguas de Pasto. Hoy guarda artes y tradiciones populares como el barniz de Pasto, la técnica del mopa mopa reconocida por la UNESCO."),
        PointOfInterest("h7", "Templo de Lourdes", "Calle 13, San Andresito", LatLng(1.215004, -77.284170), PoiCategory.TEMPLO,
            "Templo del sector de San Andresito, vecino al Museo Taminango."),
        PointOfInterest("h8", "Templo de Santiago Apóstol", "Calle 13, Santiago", LatLng(1.210865, -77.282190), PoiCategory.TEMPLO,
            "Templo sobre una colina con vista a la ciudad y al volcán Galeras.",
            "Llegaste al Templo de Santiago Apóstol. Desde esta loma se tiene una de las mejores vistas del centro de Pasto y, en días despejados, del volcán Galeras."),
        PointOfInterest("h9", "Templo de Cristo Rey", "Carrera 24, Hullaguanga", LatLng(1.214966, -77.276650), PoiCategory.TEMPLO,
            "Templo de estilo neogótico en pleno centro."),
        PointOfInterest("h10", "Templo de La Merced", "Carrera 22, La Panadería", LatLng(1.212175, -77.278410), PoiCategory.TEMPLO,
            "Santuario de la Virgen de las Mercedes, patrona de Pasto.",
            "Este es el templo de La Merced, casa de la Virgen de las Mercedes, patrona de la ciudad de Pasto."),
        PointOfInterest("h11", "Plaza del Carnaval", "El Churo", LatLng(1.210561, -77.276550), PoiCategory.CULTURA,
            "Escenario del Carnaval de Negros y Blancos.",
            "Estás en la Plaza del Carnaval, escenario del Carnaval de Negros y Blancos, declarado Patrimonio Cultural Inmaterial de la Humanidad por la UNESCO en 2009."),
        PointOfInterest("h12", "Museo del Oro (Banco de la República)", "Calle 19, El Churo", LatLng(1.211616, -77.276680), PoiCategory.CULTURA,
            "Orfebrería y cerámica prehispánica de Nariño.",
            "Aquí está el Museo del Oro del Banco de la República, con piezas de orfebrería y cerámica de los pueblos prehispánicos de Nariño."),
        PointOfInterest("h13", "Plazoleta Rumipamba", "Centro", LatLng(1.216665, -77.281460), PoiCategory.HISTORIA,
            "Plazoleta peatonal; su nombre viene del quechua: llanura de piedra."),

        // Parques
        PointOfInterest("p1", "Parque Infantil", "Comuna 9", LatLng(1.219430, -77.281550), PoiCategory.PARQUE,
            "Parque verde y recreativo cerca del centro."),
        PointOfInterest("p2", "Parque Bolívar", "Comuna 2", LatLng(1.205584, -77.270270), PoiCategory.PARQUE,
            "Parque tradicional del sur del centro."),
        PointOfInterest("p3", "Parque de los Periodistas", "Avenida Santander", LatLng(1.210650, -77.274020), PoiCategory.PARQUE,
            "Pequeño parque sobre la Avenida Santander."),
        PointOfInterest("p4", "Parque Santiago", "Comuna 1", LatLng(1.210640, -77.282770), PoiCategory.PARQUE,
            "Parque junto al Templo de Santiago."),
        PointOfInterest("p5", "Parque de Toledo", "Comuna 1", LatLng(1.217222, -77.274580), PoiCategory.PARQUE,
            "Parque de barrio al nororiente del centro."),
        PointOfInterest("d1", "Estadio Departamental Libertad", "Carrera 9, Chapal", LatLng(1.198084, -77.277890), PoiCategory.PARQUE,
            "Casa del Deportivo Pasto."),

        // Comercio y comida
        PointOfInterest("c1", "C.C. Unicentro", "Carrera 34, La Aurora", LatLng(1.216309, -77.288480), PoiCategory.COMERCIO,
            "Centro comercial con tiendas, cine y plazoleta de comidas."),
        PointOfInterest("c2", "C.C. Galerías", "Calle 19, Las Cuadras", LatLng(1.216390, -77.278940), PoiCategory.COMERCIO,
            "Centro comercial del centro de la ciudad."),
        PointOfInterest("c3", "C.C. Único Outlet", "Carrera 3 Este, Los Sauces", LatLng(1.205689, -77.259140), PoiCategory.COMERCIO,
            "Centro comercial de outlets al oriente de la ciudad."),
        PointOfInterest("f1", "Míster Pollo (Calle 18)", "Calle 18, Universitario", LatLng(1.226914, -77.287470), PoiCategory.COMIDA,
            "Restaurante tradicional pastuso."),
        PointOfInterest("f2", "Míster Pollo Centro", "Carrera 21A, San Andresito", LatLng(1.211433, -77.277610), PoiCategory.COMIDA,
            "Sede del centro, cerca de la Plaza del Carnaval."),

        // Educación y salud
        PointOfInterest("e1", "Universidad de Nariño (Torobajo)", "Acceso Universidad, Torobajo", LatLng(1.232534, -77.293380), PoiCategory.EDUCACION,
            "Sede principal de la universidad pública del departamento.",
            "Llegaste a la Universidad de Nariño, sede Torobajo, la universidad pública del departamento y centro de la vida académica de Pasto."),
        PointOfInterest("e2", "Universidad Mariana", "Calle 19, Palermo", LatLng(1.223697, -77.283180), PoiCategory.EDUCACION,
            "Universidad del barrio Palermo."),
        PointOfInterest("e3", "Avenida de Los Estudiantes", "Versalles", LatLng(1.227103, -77.282640), PoiCategory.CULTURA,
            "Corredor universitario con cafés y restaurantes."),
        PointOfInterest("e4", "Biblioteca Carlos César Puyana", "Carrera 30, Maridíaz", LatLng(1.219118, -77.281710), PoiCategory.EDUCACION,
            "Biblioteca pública municipal."),
        PointOfInterest("s1", "Hospital San Pedro", "Carrera 43, Villas de San Rafael", LatLng(1.224731, -77.292010), PoiCategory.SALUD,
            "Fundación Hospital San Pedro."),
        PointOfInterest("s2", "Hospital Infantil Los Ángeles", "Av. de Los Estudiantes, Versalles", LatLng(1.223290, -77.280010), PoiCategory.SALUD,
            "Atención pediátrica especializada."),
        PointOfInterest("t1", "Terminal de Transportes", "Calle 16B, Emilio Botero", LatLng(1.198398, -77.269260), PoiCategory.TRANSPORTE,
            "Buses intermunicipales y hacia Ecuador."),

        // Fuera de Pasto (para explorar en el mapa; demasiado lejos para ir a pie)
        PointOfInterest("x1", "Santuario de Las Lajas", "Ipiales", LatLng(0.80530, -77.58610), PoiCategory.TEMPLO,
            "Basílica neogótica sobre el cañón del río Guáitara."),
        PointOfInterest("x2", "Laguna de La Cocha (El Puerto)", "El Encano", LatLng(1.15500, -77.16300), PoiCategory.NATURALEZA,
            "Humedal Ramsar y pueblo de casas de madera."),
        PointOfInterest("x3", "Volcán Galeras", "Santuario de Fauna y Flora", LatLng(1.22000, -77.37000), PoiCategory.NATURALEZA,
            "Volcán activo que vigila la ciudad (4.276 msnm)."),
        PointOfInterest("x4", "Basílica de Sandoná", "Sandoná", LatLng(1.28694, -77.47222), PoiCategory.TEMPLO,
            "Iglesia de piedra en la tierra del sombrero de paja toquilla."),
        PointOfInterest("x5", "Aeropuerto Antonio Nariño", "Chachagüí", LatLng(1.39625, -77.29150), PoiCategory.TRANSPORTE,
            "Aeropuerto que sirve a Pasto.")
    )

    private val poiById = pois.associateBy { it.id }
    fun poi(id: String): PointOfInterest? = poiById[id]

    // --- RUTAS RECOMENDADAS: TODAS A PIE DENTRO DE PASTO, CON PARADAS EN ORDEN ---
    val historicalRoutes = listOf(
        HistoricalRoute(
            id = "r1",
            title = "Corazón Colonial",
            subtitle = "De la Plaza de Nariño al Museo Taminango",
            tag = "CULTURA", tagColorHex = "#DB2777",
            stopIds = listOf("h1", "h2", "h3", "h5", "h7", "h6"),
            suggestedMood = "Andino y acústico"
        ),
        HistoricalRoute(
            id = "r2",
            title = "Circuito del Carnaval",
            subtitle = "Plaza del Carnaval → Museo del Oro → Plaza de Nariño → Parque Infantil",
            tag = "PATRIMONIO", tagColorHex = "#B45309",
            stopIds = listOf("h11", "h12", "h10", "h1", "h13", "p1"),
            suggestedMood = "Cumbia y fiesta"
        ),
        HistoricalRoute(
            id = "r3",
            title = "Templos y Miradores",
            subtitle = "Cristo Rey → San Juan → San Agustín → La Merced → Santiago",
            tag = "ARQUITECTURA", tagColorHex = "#7C3AED",
            stopIds = listOf("h9", "h2", "h4", "h10", "h8"),
            suggestedMood = "Lo-fi tranquilo"
        ),
        HistoricalRoute(
            id = "r4",
            title = "Parques del Sur al Norte",
            subtitle = "Parque Bolívar → Periodistas → Plaza del Carnaval → Santiago → Infantil",
            tag = "NATURALEZA", tagColorHex = "#16A34A",
            stopIds = listOf("p2", "p3", "h11", "p4", "p1"),
            suggestedMood = "Indie y pop"
        ),
        HistoricalRoute(
            id = "r5",
            title = "Ruta Universitaria",
            subtitle = "Unicentro → U. Mariana → Av. Estudiantes → Torobajo",
            tag = "JUVENTUD", tagColorHex = "#0891B2",
            stopIds = listOf("c1", "e2", "e3", "f1", "e1"),
            suggestedMood = "Electrónica con energía"
        )
    )

    fun stopsOf(route: HistoricalRoute): List<PointOfInterest> = route.stopIds.mapNotNull { poi(it) }

    // --- ESTACIONES DE RADIO (streams públicos) ---
    val radioStations = listOf(
        RadioStation("st_lofi", "Lo-Fi Peatonal", "Chill beats", 85,
            "https://ice1.somafm.com/fluid-128-mp3", Pair("#10B981", "#047857")),
        RadioStation("st_groove", "Groove Andino", "Downtempo & house suave", 110,
            "https://ice1.somafm.com/groovesalad-128-mp3", Pair("#F59E0B", "#B45309")),
        RadioStation("st_indie", "Indie Caminante", "Indie pop & rock", 120,
            "https://ice1.somafm.com/indiepop-128-mp3", Pair("#8B5CF6", "#5B21B6")),
        RadioStation("st_cardio", "Cadencia Alta", "Beats electrónicos", 135,
            "https://ice1.somafm.com/beatblender-128-mp3", Pair("#EF4444", "#B91C1C"))
    )
}
