package com.example.georhythm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.georhythm.core.GeoRhythmController
import com.example.georhythm.data.AuthOutcome
import com.example.georhythm.data.LocalData
import com.example.georhythm.data.PointOfInterest
import com.example.georhythm.ui.auth.AuthScreen
import com.example.georhythm.ui.components.GlassCard
import com.example.georhythm.ui.components.SearchDestinationDialog
import com.example.georhythm.ui.screens.CaminanteScreen
import com.example.georhythm.ui.screens.ExplorarScreen
import com.example.georhythm.ui.screens.RutasViejasScreen
import com.example.georhythm.ui.screens.TuMusicaScreen
import com.example.georhythm.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val pendingRedirect = mutableStateOf<Uri?>(null)
    private val pendingAuthLink = mutableStateOf<Uri?>(null)
    private val controller get() = (application as GeoRhythmApp).controller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureRedirect(intent)

        val uiPrefs = getSharedPreferences("georhythm_ui", Context.MODE_PRIVATE)
        setContent {
            var isDark by remember { mutableStateOf(uiPrefs.getBoolean("dark", true)) }
            val toggleTheme = {
                isDark = !isDark
                uiPrefs.edit().putBoolean("dark", isDark).apply()
            }
            GeoRhythmTheme(darkTheme = isDark) {
                val userManager = controller.userManager
                var isLoggedIn by remember { mutableStateOf(userManager.isLoggedIn()) }
                LaunchedEffect(isLoggedIn) { if (isLoggedIn) userManager.refresh() }

                // Enlace del correo de confirmación de Supabase → iniciar sesión automáticamente
                LaunchedEffect(pendingAuthLink.value) {
                    val uri = pendingAuthLink.value ?: return@LaunchedEffect
                    pendingAuthLink.value = null
                    // Igual que con Spotify: procesar fuera del efecto para que no se cancele
                    controller.scope.launch {
                    val out = userManager.loginFromEmailLink(uri.fragment ?: uri.query ?: "")
                    val msg = when (out) {
                        is AuthOutcome.Success -> { isLoggedIn = true; "¡Correo confirmado! Bienvenido a GeoRhythm." }
                        is AuthOutcome.Failure -> out.message
                        AuthOutcome.NeedsEmailConfirmation -> null
                    }
                    msg?.let { android.widget.Toast.makeText(this@MainActivity, it, android.widget.Toast.LENGTH_LONG).show() }
                    }
                }

                Crossfade(targetState = isLoggedIn, label = "auth") { logged ->
                    if (!logged) {
                        AuthScreen(userManager) { isLoggedIn = true }
                    } else {
                        GeoRhythmMainApp(
                            controller = controller,
                            pendingRedirect = pendingRedirect,
                            isDark = isDark,
                            onToggleTheme = toggleTheme,
                            onLogout = { controller.onLogout(); isLoggedIn = false }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller.setUiVisible(true)
    }

    override fun onStop() {
        // Si hay ruta o música, el WalkService mantiene el GPS; si no, se apaga para ahorrar batería
        controller.setUiVisible(false)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureRedirect(intent)
    }

    private fun captureRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "georhythm" && uri.host == "callback") pendingRedirect.value = uri
        if (uri.scheme == "georhythm" && uri.host == "auth-callback") pendingAuthLink.value = uri
    }
}

@Composable
fun GeoRhythmMainApp(
    controller: GeoRhythmController,
    pendingRedirect: MutableState<Uri?>,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val userManager = controller.userManager
    val player = controller.player
    val tracker = controller.tracker

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var previewStops by remember { mutableStateOf<List<PointOfInterest>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(controller.hasLocationPermission()) }

    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }

    // Mensajes del controlador → snackbar
    LaunchedEffect(Unit) {
        controller.messages.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(msg) }
        }
    }
    // Al empezar una ruta nueva, volver al mapa
    LaunchedEffect(controller.routeStartCount) {
        if (controller.routeStartCount > 0) { selectedTab = 0; previewStops = emptyList() }
    }

    // Permisos: ubicación (+ notificaciones en Android 13+ para la caminata en segundo plano)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = controller.hasLocationPermission()
        controller.updateLocationUpdates()
        if (!hasPermission) controller.notify("Sin ubicación no podemos guiarte ni medir tu ritmo.")
    }
    val requestPermission = {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(perms.toTypedArray())
    }
    LaunchedEffect(Unit) { if (!hasPermission) requestPermission() }

    // Retorno del login de Spotify (georhythm://callback)
    LaunchedEffect(pendingRedirect.value) {
        val uri = pendingRedirect.value ?: return@LaunchedEffect
        // Se procesa en el scope del controlador: si se hiciera aquí, al poner el valor en null
        // el LaunchedEffect se reiniciaría y cancelaría el intercambio del código a medio camino.
        pendingRedirect.value = null
        selectedTab = 2
        controller.scope.launch {
            val err = player.handleSpotifyRedirect(uri)
            // El error queda fijo en la pestaña Música para poder leerlo
            player.errorMessage = err?.let { "Spotify: $it" }
            controller.notify(err ?: "¡Spotify conectado!")
        }
    }
    val connectSpotify = {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, player.spotifyLoginUri())) }
            .onFailure { controller.notify("No se pudo abrir el navegador.") }
        Unit
    }

    Scaffold(
        containerColor = LocalGeoColors.current.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { GeoBottomBar(selectedTab) { selectedTab = it } }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            // El mapa permanece siempre compuesto (no se recrea el contexto OpenGL al cambiar de pestaña)
            ExplorarScreen(
                currentLocation = controller.currentLocation,
                hasLocationPermission = hasPermission,
                tracker = tracker,
                player = player,
                isDark = isDark,
                discoveredIds = userManager.discoveredPoiIds,
                favorites = userManager.favorites,
                previewStops = previewStops,
                storyPoi = controller.storyPoi,
                isNarrating = controller.isNarrating,
                onToggleTheme = onToggleTheme,
                onOpenSearch = { showSearch = true },
                onNavigateTo = { poi -> controller.startNavigation(poi.latLng, poi.name) },
                onCancelNavigation = { controller.finishNavigation(arrived = false) },
                onOpenMusic = { selectedTab = 2 },
                onToggleFavorite = { poi -> scope.launch { userManager.toggleFavorite(poi.name, poi.latLng.latitude, poi.latLng.longitude) } },
                onNarrate = { controller.narrate(it) },
                onDismissStory = { controller.dismissStory() },
                onRequestPermission = { requestPermission() }
            )

            when (selectedTab) {
                1 -> RutasViejasScreen(
                    currentLocation = controller.currentLocation,
                    discoveredIds = userManager.discoveredPoiIds,
                    onPreview = { r -> previewStops = LocalData.stopsOf(r); selectedTab = 0 },
                    onStartRoute = { r -> controller.startRecommendedRoute(r) }
                )
                2 -> TuMusicaScreen(player = player, onConnectSpotify = connectSpotify)
                3 -> CaminanteScreen(
                    userManager = userManager,
                    speedKmh = tracker.speedKmh,
                    cadenceSpm = tracker.cadenceSpm,
                    isDark = isDark,
                    onToggleTheme = onToggleTheme,
                    onNavigateToFavorite = { f -> controller.startNavigation(f.latLng, f.name) },
                    onCreateAccount = { scope.launch { userManager.logout(); onLogout() } },
                    onLogout = onLogout
                )
            }

            if (controller.isCalculatingRoute) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    GlassCard(corner = 20.dp) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Emerald, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(14.dp))
                            Text("Trazando ruta a pie…", color = LocalGeoColors.current.text, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showSearch) {
        SearchDestinationDialog(
            currentLocation = controller.currentLocation,
            favorites = userManager.favorites,
            onDismiss = { showSearch = false },
            onSelectDestination = { d ->
                showSearch = false
                previewStops = emptyList()
                controller.startNavigation(d.latLng, d.name)
            }
        )
    }
}

@Composable
private fun GeoBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalGeoColors.current
    val items = listOf(
        Triple("Explorar", Icons.Default.Explore, Emerald),
        Triple("Rutas", Icons.Default.Route, Emerald),
        Triple("Música", Icons.Default.LibraryMusic, Violet),
        Triple("Caminante", Icons.Default.Person, Emerald)
    )
    NavigationBar(containerColor = c.surface, tonalElevation = 0.dp) {
        items.forEachIndexed { i, (label, icon, accent) ->
            NavigationBarItem(
                selected = selected == i,
                onClick = { onSelect(i) },
                icon = {
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(if (selected == i) accent.copy(alpha = 0.16f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) { Icon(icon, label) }
                },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent, selectedTextColor = accent,
                    unselectedIconColor = c.textMuted, unselectedTextColor = c.textMuted,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
