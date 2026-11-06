package com.example.georhythm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.georhythm.audio.MusicPlayerEngine
import com.example.georhythm.audio.MusicSource
import com.example.georhythm.data.AudiusTrack
import com.example.georhythm.data.SpotifyTrack
import com.example.georhythm.network.AudiusService
import com.example.georhythm.ui.components.Pill
import com.example.georhythm.ui.components.ScreenHeader
import com.example.georhythm.ui.components.parseHexColor
import com.example.georhythm.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TuMusicaScreen(
    player: MusicPlayerEngine,
    onConnectSpotify: () -> Unit
) {
    val c = LocalGeoColors.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Audius
    var query by rememberSaveable { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<AudiusTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var activeChip by rememberSaveable { mutableStateOf("Tendencias") }

    // Spotify
    var spQuery by rememberSaveable { mutableStateOf("") }
    var spTracks by remember { mutableStateOf<List<SpotifyTrack>>(emptyList()) }
    var spLoading by remember { mutableStateOf(false) }

    fun searchAudius(term: String?, genre: String? = null) {
        keyboard?.hide()
        loading = true
        scope.launch {
            tracks = when {
                !term.isNullOrBlank() -> AudiusService.search(term)
                else -> AudiusService.trending(genre)
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { if (tracks.isEmpty()) searchAudius(null) }

    LazyColumn(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenHeader("Tu Música", "La banda sonora que sigue el ritmo de tus pasos") }
        item { RhythmCard(player) }

        player.errorMessage?.let { msg ->
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Danger.copy(alpha = 0.12f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = Danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = c.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { player.errorMessage = null }, Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Cerrar", tint = c.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Selector de fuente
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surfaceAlt).padding(4.dp)
            ) {
                SourceTab("Buscar", Icons.Default.Search, tab == 0, Violet, Modifier.weight(1f)) { tab = 0 }
                SourceTab("Radio", Icons.Default.Radio, tab == 1, Emerald, Modifier.weight(1f)) { tab = 1 }
                SourceTab("Spotify", Icons.AutoMirrored.Filled.QueueMusic, tab == 2, SpotifyGreen, Modifier.weight(1f)) { tab = 2 }
            }
        }

        when (tab) {
            // ---------------------------------------------------------- AUDIUS
            0 -> {
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Canción, artista o género…", color = c.textMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Violet) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = ""; activeChip = "Tendencias"; searchAudius(null) }) {
                                Icon(Icons.Default.Close, "Limpiar", tint = c.textMuted)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { activeChip = ""; searchAudius(query) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet, unfocusedBorderColor = c.border,
                            focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                            focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = Violet
                        )
                    )
                }
                item {
                    val chips = listOf(
                        "Tendencias" to null, "Electrónica" to "Electronic", "Latina" to "Latin",
                        "Hip-Hop" to "Hip-Hop/Rap", "Pop" to "Pop", "Lo-Fi" to "Lo-Fi", "Alternativa" to "Alternative"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(chips) { (label, genre) ->
                            FilterChip(
                                selected = activeChip == label,
                                onClick = { activeChip = label; query = ""; searchAudius(null, genre) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Violet)
                        }
                    }
                } else if (tracks.isEmpty()) {
                    item {
                        Text("No hay resultados. Revisa tu conexión o prueba otra búsqueda.", color = c.textMuted,
                            fontSize = 13.sp, modifier = Modifier.padding(vertical = 30.dp))
                    }
                } else {
                    items(tracks, key = { it.id }) { t ->
                        TrackRow(
                            title = t.title, subtitle = t.artist, imageUrl = t.artworkUrl,
                            bpm = t.bpm, cadence = player.cadenceSpm,
                            isCurrent = player.currentSource == MusicSource.AUDIUS && player.currentAudiusTrack?.id == t.id,
                            isPlaying = player.isPlaying, accent = Violet
                        ) { player.playAudius(t, tracks) }
                    }
                    item {
                        Text("Música de Audius, catálogo abierto de artistas independientes.", color = c.textMuted,
                            fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            // ---------------------------------------------------------- RADIO
            1 -> {
                items(player.stations, key = { it.id }) { st ->
                    val current = player.currentSource == MusicSource.RADIO && player.activeStation?.id == st.id
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface)
                            .border(1.dp, if (current) Emerald else c.border, RoundedCornerShape(18.dp))
                            .clickable { player.playStation(st) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(
                                Brush.linearGradient(listOf(parseHexColor(st.coverGradientHex.first), parseHexColor(st.coverGradientHex.second)))
                            ),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Radio, null, tint = Color.White) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(st.name, color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(st.genre, color = c.textMuted, fontSize = 12.sp)
                        }
                        Pill("${st.targetBpm} BPM", Emerald)
                        Spacer(Modifier.width(6.dp))
                        PlayDot(current, player.isPlaying, Emerald)
                    }
                }
                item {
                    Text("Con Ritmo al paso activo, la radio cambia a la emisora que mejor se ajusta a tu cadencia.",
                        color = c.textMuted, fontSize = 12.sp)
                }
            }

            // ---------------------------------------------------------- SPOTIFY
            2 -> {
                if (!player.spotifyConnected) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface)
                                .border(1.dp, c.border, RoundedCornerShape(20.dp)).padding(18.dp)
                        ) {
                            Text("Conecta Spotify", color = c.text, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Busca canciones, abre tus playlists y deja que GeoRhythm cambie de playlist según tu ritmo. " +
                                    "Controlar la reproducción desde aquí requiere Spotify Premium.",
                                color = c.textMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Button(
                                onClick = onConnectSpotify,
                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                            ) { Text("Conectar con Spotify", fontWeight = FontWeight.Bold) }
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = spQuery, onValueChange = { spQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar en Spotify…", color = c.textMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = SpotifyGreen) },
                            singleLine = true, shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboard?.hide()
                                spLoading = true
                                scope.launch { spTracks = player.searchSpotify(spQuery); spLoading = false }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpotifyGreen, unfocusedBorderColor = c.border,
                                focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                                focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = SpotifyGreen
                            )
                        )
                    }
                    if (spLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SpotifyGreen) }
                    items(spTracks, key = { "t" + it.id }) { t ->
                        TrackRow(t.title, t.artist, t.imageUrl, null, 0, false, false, SpotifyGreen) { player.playSpotifyTrack(t) }
                    }
                    if (player.spotifyPlaylists.isNotEmpty()) {
                        item { Text("Tus playlists", color = c.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                        items(player.spotifyPlaylists, key = { "p" + it.uri }) { p ->
                            TrackRow(p.name, "${p.tracksCount} canciones", p.imageUrl, null, 0, false, false, SpotifyGreen) {
                                player.playSpotifyPlaylist(p)
                            }
                        }
                    }
                    item {
                        TextButton(onClick = { player.disconnectSpotify() }) {
                            Text("Desconectar Spotify", color = c.textMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RhythmCard(player: MusicPlayerEngine) {
    val c = LocalGeoColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF0E7490), Color(0xFF4C1D95))))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RITMO AL PASO", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                Text(
                    if (player.cadenceSpm > 0) "${player.cadenceSpm} pasos/min" else "Empieza a caminar",
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(player.paceTier.label, color = Mint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Switch(
                checked = player.rhythmSyncEnabled,
                onCheckedChange = { player.setRhythmSync(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = Emerald, checkedThumbColor = Color.White)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (player.rhythmSyncEnabled)
                "La música acelera o desacelera (sin cambiar el tono) para ir a tu paso, y la siguiente canción se elige con el BPM más cercano a tu cadencia."
            else "Activa para que la música se adapte a tu velocidad.",
            color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 16.sp
        )
        if (player.rhythmSyncEnabled && player.currentSource == MusicSource.AUDIUS && player.trackBpm != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Tempo: ${player.trackBpm} BPM × ${"%.2f".format(player.playbackRate)}",
                color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { player.startPaceMix() },
            enabled = !player.isLoadingPool,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF064E3B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (player.isLoadingPool) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = EmeraldDeep)
            else Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Mezcla para mi paso", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SourceTab(label: String, icon: ImageVector, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalGeoColors.current
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) Color.White else c.textMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) Color.White else c.textMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun TrackRow(
    title: String,
    subtitle: String,
    imageUrl: String?,
    bpm: Int?,
    cadence: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val c = LocalGeoColors.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(c.surfaceAlt), contentAlignment = Alignment.Center) {
            if (imageUrl != null) AsyncImage(imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.MusicNote, null, tint = c.textMuted)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (isCurrent) accent else c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = c.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (bpm != null) {
            // Verde si se puede ajustar a tu paso actual (±12 %)
            val matches = cadence > 0 && listOf(bpm, bpm * 2, bpm / 2).any { abs(it - cadence) <= cadence * 0.12f }
            Pill("$bpm BPM", if (matches) Emerald else c.textMuted)
            Spacer(Modifier.width(6.dp))
        }
        PlayDot(isCurrent, isPlaying, accent)
    }
}

@Composable
private fun PlayDot(isCurrent: Boolean, isPlaying: Boolean, accent: Color) {
    val c = LocalGeoColors.current
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(if (isCurrent) accent else c.surfaceAlt),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow, null,
            tint = if (isCurrent) Color.White else c.text, modifier = Modifier.size(18.dp)
        )
    }
}
