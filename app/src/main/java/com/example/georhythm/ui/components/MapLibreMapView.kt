package com.example.georhythm.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.georhythm.BuildConfig
import com.example.georhythm.data.LocalData
import com.example.georhythm.data.MapLayerStyle
import com.example.georhythm.data.NavigationRouteData
import com.example.georhythm.data.PointOfInterest
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val SRC_POIS = "geo-pois"
private const val SRC_ROUTE = "geo-route"
private const val SRC_STOPS = "geo-stops"
private const val LYR_POIS = "geo-pois-dots"
private const val LYR_POI_LABELS = "geo-pois-labels"
private const val LYR_ROUTE_CASING = "geo-route-casing"
private const val LYR_ROUTE = "geo-route-line"
private const val LYR_STOPS = "geo-stops-dots"
private const val LYR_STOP_NUMBERS = "geo-stops-numbers"

/**
 * Mapa MapLibre persistente. Los lugares se dibujan como puntos pequeños de color (capa GeoJSON)
 * en vez de pines grandes, y los nombres sólo aparecen al acercarse (zoom ≥ 15.5).
 */
@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    mapLayerStyle: MapLayerStyle,
    hasLocationPermission: Boolean,
    currentLocation: Location?,
    activeRoute: NavigationRouteData?,
    previewStops: List<PointOfInterest>,
    discoveredIds: Set<String>,
    isAutoTracking: Boolean,
    isNavigating: Boolean,
    onUserGesturedMap: () -> Unit,
    onMapReady: (MapLibreMap) -> Unit,
    onPoiSelected: (PointOfInterest) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }

    // Callbacks actualizados sin re-registrar listeners
    val gestureCb by rememberUpdatedState(onUserGesturedMap)
    val poiCb by rememberUpdatedState(onPoiSelected)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            runCatching {
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching {
                map?.let { disableLocationComponent(it) }
                mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    // Cambiar estilo: vuelve a crear las capas propias en el nuevo estilo
    LaunchedEffect(mapLayerStyle, map) {
        val m = map ?: return@LaunchedEffect
        loadedStyle = null
        val url = "https://api.maptiler.com/maps/${mapLayerStyle.styleName}/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
        m.setStyle(Style.Builder().fromUri(url)) { style ->
            addGeoLayers(style)
            loadedStyle = style
        }
    }

    // Componente de ubicación (punto azul con dirección). Usa nuestras lecturas GPS: un solo proveedor.
    LaunchedEffect(loadedStyle, hasLocationPermission) {
        val m = map ?: return@LaunchedEffect
        val style = loadedStyle ?: return@LaunchedEffect
        if (!hasLocationPermission || !hasAnyLocationPermission(context)) return@LaunchedEffect
        enableLocationComponent(m, style, context, currentLocation)
    }

    // Datos de los puntos (se actualiza al descubrir lugares)
    LaunchedEffect(loadedStyle, discoveredIds) {
        val style = loadedStyle ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>(SRC_POIS)?.setGeoJson(poiFeatures(discoveredIds))
    }

    // Ruta activa o vista previa de paradas
    LaunchedEffect(loadedStyle, activeRoute, previewStops) {
        val style = loadedStyle ?: return@LaunchedEffect
        val line = activeRoute?.points?.takeIf { it.size >= 2 }?.let {
            Feature.fromGeometry(LineString.fromLngLats(it.map { p -> Point.fromLngLat(p.longitude, p.latitude) }))
        }
        style.getSourceAs<GeoJsonSource>(SRC_ROUTE)
            ?.setGeoJson(FeatureCollection.fromFeatures(listOfNotNull(line)))

        val stops = activeRoute?.waypoints?.takeIf { it.isNotEmpty() } ?: previewStops
        val stopFeatures = stops.mapIndexed { i, s ->
            Feature.fromGeometry(Point.fromLngLat(s.latLng.longitude, s.latLng.latitude)).apply {
                addStringProperty("n", "${i + 1}")
            }
        }.toMutableList()
        // Destino final de una navegación simple (sin paradas)
        if (activeRoute != null && activeRoute.waypoints.isEmpty()) {
            stopFeatures += Feature.fromGeometry(
                Point.fromLngLat(activeRoute.destinationLatLng.longitude, activeRoute.destinationLatLng.latitude)
            ).apply { addStringProperty("n", "★") }
        }
        style.getSourceAs<GeoJsonSource>(SRC_STOPS)?.setGeoJson(FeatureCollection.fromFeatures(stopFeatures))
    }

    // Ubicación + seguimiento de cámara
    LaunchedEffect(currentLocation, isAutoTracking, map) {
        val m = map ?: return@LaunchedEffect
        val loc = currentLocation
        if (loc != null && loadedStyle != null) {
            runCatching { m.locationComponent.takeIf { it.isLocationComponentActivated }?.forceLocationUpdate(loc) }
        }
        if (!isAutoTracking || !isLocationInPastoRegion(loc)) return@LaunchedEffect
        val current = m.cameraPosition
        val pos = CameraPosition.Builder()
            .target(LatLng(loc!!.latitude, loc.longitude))
            .zoom(if (isNavigating) maxOf(current.zoom, 17.0) else current.zoom)
            .tilt(if (isNavigating) 55.0 else current.tilt)
            .bearing(if (isNavigating && loc.hasBearing() && loc.speed > 0.6f) loc.bearing.toDouble() else current.bearing)
            .build()
        m.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 700)
    }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(null)
                getMapAsync { m ->
                    m.uiSettings.isCompassEnabled = true
                    m.uiSettings.isAttributionEnabled = true
                    m.uiSettings.isLogoEnabled = false
                    m.cameraPosition = CameraPosition.Builder().target(PASTO_CENTER).zoom(15.5).tilt(40.0).build()
                    m.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) gestureCb()
                    }
                    m.addOnMapClickListener { latLng ->
                        val pt = m.projection.toScreenLocation(latLng)
                        val hit = m.queryRenderedFeatures(
                            android.graphics.RectF(pt.x - 24, pt.y - 24, pt.x + 24, pt.y + 24), LYR_POIS
                        ).firstOrNull()
                        val poi = hit?.getStringProperty("id")?.let { LocalData.poi(it) }
                        if (poi != null) { poiCb(poi); true } else false
                    }
                    map = m
                    onMapReady(m)
                }
            }
        },
        modifier = modifier
    )
}

@SuppressLint("MissingPermission") // apagar el componente no requiere permiso
private fun disableLocationComponent(m: MapLibreMap) {
    m.locationComponent.takeIf { it.isLocationComponentActivated }?.isLocationComponentEnabled = false
}

@SuppressLint("MissingPermission") // el permiso se verifica antes de llamar
private fun enableLocationComponent(m: MapLibreMap, style: Style, context: android.content.Context, loc: Location?) {
    runCatching {
        val lc = m.locationComponent
        lc.activateLocationComponent(
            LocationComponentActivationOptions.builder(context, style).useDefaultLocationEngine(false).build()
        )
        lc.isLocationComponentEnabled = true
        lc.cameraMode = CameraMode.NONE
        lc.renderMode = RenderMode.GPS
        loc?.let { lc.forceLocationUpdate(it) }
    }
}

private fun hasAnyLocationPermission(context: android.content.Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun poiFeatures(discovered: Set<String>): FeatureCollection =
    FeatureCollection.fromFeatures(LocalData.pois.map { poi ->
        Feature.fromGeometry(Point.fromLngLat(poi.latLng.longitude, poi.latLng.latitude)).apply {
            addStringProperty("id", poi.id)
            addStringProperty("name", poi.name)
            addStringProperty("color", poi.category.colorHex)
            addBooleanProperty("found", poi.id in discovered)
        }
    })

private fun addGeoLayers(style: Style) {
    style.addSource(GeoJsonSource(SRC_POIS, FeatureCollection.fromFeatures(emptyList())))
    style.addSource(GeoJsonSource(SRC_ROUTE, FeatureCollection.fromFeatures(emptyList())))
    style.addSource(GeoJsonSource(SRC_STOPS, FeatureCollection.fromFeatures(emptyList())))

    // Ruta: borde + línea azul de navegación
    style.addLayer(
        LineLayer(LYR_ROUTE_CASING, SRC_ROUTE).withProperties(
            lineColor("#0B4FB3"), lineWidth(Expression.interpolate(Expression.linear(), Expression.zoom(),
                Expression.stop(12, 5f), Expression.stop(18, 13f))),
            lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    style.addLayer(
        LineLayer(LYR_ROUTE, SRC_ROUTE).withProperties(
            lineColor("#1A73E8"), lineWidth(Expression.interpolate(Expression.linear(), Expression.zoom(),
                Expression.stop(12, 3f), Expression.stop(18, 9f))),
            lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)
        )
    )

    // Lugares: puntos pequeños (3–7 px según zoom) con borde blanco
    style.addLayer(
        CircleLayer(LYR_POIS, SRC_POIS).withProperties(
            circleColor(Expression.get("color")),
            circleRadius(Expression.interpolate(Expression.linear(), Expression.zoom(),
                Expression.stop(10, 2.5f), Expression.stop(14, 4.5f), Expression.stop(18, 7f))),
            circleStrokeColor("#FFFFFF"),
            circleStrokeWidth(1.5f),
            circleOpacity(Expression.switchCase(Expression.get("found"), Expression.literal(0.55f), Expression.literal(1f)))
        )
    )
    style.addLayer(
        SymbolLayer(LYR_POI_LABELS, SRC_POIS).withProperties(
            textField(Expression.get("name")),
            textFont(arrayOf("Noto Sans Regular")),
            textSize(11f),
            textColor("#334155"),
            textHaloColor("#FFFFFF"),
            textHaloWidth(1.4f),
            textOffset(arrayOf(0f, 1.1f)),
            textAnchor(Property.TEXT_ANCHOR_TOP),
            textOptional(true)
        ).apply { minZoom = 15.5f }
    )

    // Paradas numeradas de la ruta
    style.addLayer(
        CircleLayer(LYR_STOPS, SRC_STOPS).withProperties(
            circleColor("#10B981"), circleRadius(9f), circleStrokeColor("#FFFFFF"), circleStrokeWidth(2f)
        )
    )
    style.addLayer(
        SymbolLayer(LYR_STOP_NUMBERS, SRC_STOPS).withProperties(
            textField(Expression.get("n")), textFont(arrayOf("Noto Sans Bold")), textSize(11f),
            textColor("#FFFFFF"), textAllowOverlap(true), textIgnorePlacement(true)
        )
    )
}
