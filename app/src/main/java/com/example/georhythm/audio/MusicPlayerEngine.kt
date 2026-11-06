package com.example.georhythm.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.georhythm.data.AudiusTrack
import com.example.georhythm.data.LocalData
import com.example.georhythm.data.RadioStation
import com.example.georhythm.data.SpotifyPlaylist
import com.example.georhythm.data.SpotifyTrack
import com.example.georhythm.network.AudiusService
import com.example.georhythm.network.Http
import com.example.georhythm.network.SpotifyPlayResult
import com.example.georhythm.network.SpotifyService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class MusicSource { AUDIUS, RADIO, SPOTIFY }

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

/**
 * Reproductor unificado:
 * - AUDIUS: búsqueda + reproducción de canciones completas con ExoPlayer. Con "Ritmo al paso" activo
 *   ajusta el tempo (sin cambiar el tono) para igualar tu cadencia y elige la siguiente canción con
 *   el BPM más cercano a tu paso.
 * - RADIO: streams en vivo; con ritmo activo cambia a la estación cuyo BPM se ajusta a tu paso.
 * - SPOTIFY: control vía Web API (requiere Premium para reproducir); con ritmo activo cambia a una
 *   playlist acorde a tu nivel de paso.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class MusicPlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    val spotify: SpotifyService,
    private val onTrackStarted: (source: MusicSource, id: String, title: String, artist: String?, bpm: Int?) -> Unit
) {
    // ---------------------------------------------------------------- ESTADO OBSERVABLE
    var currentSource by mutableStateOf(MusicSource.AUDIUS)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var hasMedia by mutableStateOf(false)
        private set
    var title by mutableStateOf("Elige música para caminar")
        private set
    var artist by mutableStateOf("Busca una canción o activa Ritmo al paso")
        private set
    var coverUrl by mutableStateOf<String?>(null)
        private set
    var progressMs by mutableFloatStateOf(0f)
        private set
    var durationMs by mutableFloatStateOf(0f)
        private set
    var trackBpm by mutableStateOf<Int?>(null)
        private set
    var playbackRate by mutableFloatStateOf(1f)
        private set
    var errorMessage by mutableStateOf<String?>(null)

    // Ritmo
    var rhythmSyncEnabled by mutableStateOf(true)
        private set
    var cadenceSpm by mutableIntStateOf(0)
        private set
    var paceTier by mutableStateOf(PaceTier.STOPPED)
        private set
    var isLoadingPool by mutableStateOf(false)
        private set

    // Audius
    var currentAudiusTrack by mutableStateOf<AudiusTrack?>(null)
        private set
    private var audiusQueue: List<AudiusTrack> = emptyList()
    private var bpmPool: List<AudiusTrack> = emptyList()
    private val recentlyPlayed = ArrayDeque<String>()

    // Radio
    val stations: List<RadioStation> = LocalData.radioStations
    var activeStation by mutableStateOf<RadioStation?>(null)
        private set

    // Spotify
    var spotifyConnected by mutableStateOf(spotify.isConnected)
        private set
    var spotifyPlaylists by mutableStateOf<List<SpotifyPlaylist>>(emptyList())
        private set

    // ---------------------------------------------------------------- PLAYER
    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent(Http.USER_AGENT)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(12_000)
                    .setReadTimeoutMs(15_000)
            )
        )
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true) // pausa si se desconectan los audífonos
        .setWakeMode(C.WAKE_MODE_NETWORK)   // mantiene CPU y Wi-Fi activos con la pantalla apagada
        .build()

    private var tickJob: Job? = null
    private var tierSinceMs = 0L
    private var lastAppliedTier: PaceTier? = null
    private var mismatchSinceMs = 0L
    private var spotifyPollJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (currentSource != MusicSource.SPOTIFY) isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (currentSource == MusicSource.SPOTIFY) return
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED && currentSource == MusicSource.AUDIUS) {
                    playNextAudius()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                when (currentSource) {
                    MusicSource.AUDIUS -> {
                        errorMessage = "No se pudo reproducir \"${title}\". Saltando a la siguiente…"
                        playNextAudius()
                    }
                    MusicSource.RADIO -> errorMessage = "La emisora no responde. Prueba otra estación."
                    MusicSource.SPOTIFY -> Unit
                }
            }
        })
        startTicker()
        if (spotifyConnected) onSpotifyConnected()
    }

    // ================================================================= RITMO AL PASO

    fun setRhythmSync(enabled: Boolean) {
        rhythmSyncEnabled = enabled
        if (enabled) {
            ensureBpmPool()
            applyTempo()
        } else {
            setRate(1f)
        }
    }

    /** Llamado con cada nueva lectura de GPS. */
    fun updateCadence(spm: Int) {
        cadenceSpm = spm
        val tier = PaceTier.from(spm)
        if (tier != paceTier) {
            paceTier = tier
            tierSinceMs = SystemClock.elapsedRealtime()
        }
        applyTempo()
    }

    /** BPM efectivo del tema más cercano a la cadencia (permite medio y doble tiempo). */
    private fun effectiveBpm(bpm: Int, target: Int): Int =
        listOf(bpm, bpm * 2, bpm / 2).minBy { abs(it - target) }

    private fun applyTempo() {
        if (!rhythmSyncEnabled || currentSource != MusicSource.AUDIUS) {
            if (playbackRate != 1f) setRate(1f)
            return
        }
        val bpm = trackBpm
        if (bpm == null || cadenceSpm < PaceTier.STROLL.minSpm) {
            setRate(1f)
            return
        }
        val eff = effectiveBpm(bpm, cadenceSpm)
        val rate = (cadenceSpm.toFloat() / eff).coerceIn(0.92f, 1.10f)
        if (abs(rate - playbackRate) >= 0.01f) setRate(rate)
    }

    private fun setRate(rate: Float) {
        playbackRate = (rate * 100).roundToInt() / 100f
        // Velocidad cambia el tempo; el tono (pitch) se mantiene en 1.0
        player.playbackParameters = PlaybackParameters(playbackRate, 1f)
    }

    private fun ensureBpmPool() {
        if (bpmPool.isNotEmpty() || isLoadingPool) return
        isLoadingPool = true
        scope.launch {
            bpmPool = AudiusService.bpmPool()
            isLoadingPool = false
        }
    }

    /** Elige la canción cuyo BPM se ajusta mejor a la cadencia actual. */
    private fun pickTrackForPace(candidates: List<AudiusTrack>): AudiusTrack? {
        val target = if (cadenceSpm >= PaceTier.STROLL.minSpm) cadenceSpm else 95
        return candidates
            .filter { it.bpm != null && it.id !in recentlyPlayed && it.id != currentAudiusTrack?.id }
            .sortedBy { abs(effectiveBpm(it.bpm!!, target) - target) }
            .take(5)
            .randomOrNull()
    }

    /** Reproduce de inmediato una canción adecuada al paso actual. */
    fun startPaceMix() {
        setRhythmSync(true)
        scope.launch {
            if (bpmPool.isEmpty()) {
                isLoadingPool = true
                bpmPool = AudiusService.bpmPool()
                isLoadingPool = false
            }
            val pick = pickTrackForPace(bpmPool)
            if (pick != null) playAudius(pick, bpmPool) else errorMessage = "No se pudo cargar música. Revisa tu conexión."
        }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                if (currentSource != MusicSource.SPOTIFY) {
                    progressMs = player.currentPosition.toFloat().coerceAtLeast(0f)
                    val d = player.duration
                    if (d > 0) durationMs = d.toFloat()
                }
                checkPaceSwitch()
                delay(1000)
            }
        }
    }

    /** Cambia de canción/emisora/playlist si el paso cambió de forma sostenida. */
    private fun checkPaceSwitch() {
        if (!rhythmSyncEnabled || !isPlaying) return
        val now = SystemClock.elapsedRealtime()
        val tierStable = now - tierSinceMs > 25_000
        when (currentSource) {
            MusicSource.AUDIUS -> {
                val bpm = trackBpm ?: return
                if (cadenceSpm < PaceTier.STROLL.minSpm) { mismatchSinceMs = 0; return }
                val eff = effectiveBpm(bpm, cadenceSpm)
                val mismatch = abs(eff - cadenceSpm).toFloat() / eff > 0.14f // fuera del rango ajustable
                if (!mismatch) { mismatchSinceMs = 0; return }
                if (mismatchSinceMs == 0L) mismatchSinceMs = now
                if (now - mismatchSinceMs > 20_000 && progressMs > 30_000) {
                    mismatchSinceMs = 0
                    playNextAudius()
                }
            }
            MusicSource.RADIO -> {
                if (!tierStable || paceTier == lastAppliedTier || paceTier == PaceTier.STOPPED) return
                lastAppliedTier = paceTier
                val best = stations.minBy { abs(it.targetBpm - cadenceSpm) }
                if (best != activeStation) playStation(best)
            }
            MusicSource.SPOTIFY -> {
                if (!tierStable || paceTier == lastAppliedTier || paceTier == PaceTier.STOPPED) return
                lastAppliedTier = paceTier
                scope.launch {
                    val uri = spotify.findPlaylistUri(paceTier.spotifyQuery) ?: return@launch
                    spotify.play(contextUri = uri)
                }
            }
        }
    }

    // ================================================================= AUDIUS

    fun playAudius(track: AudiusTrack, queue: List<AudiusTrack> = audiusQueue) {
        stopSpotifyPolling()
        currentSource = MusicSource.AUDIUS
        if (queue.isNotEmpty()) audiusQueue = queue
        currentAudiusTrack = track
        activeStation = null
        title = track.title
        artist = track.artist
        coverUrl = track.artworkUrl
        trackBpm = track.bpm
        durationMs = track.durationSeconds * 1000f
        progressMs = 0f
        errorMessage = null
        hasMedia = true
        mismatchSinceMs = 0
        recentlyPlayed.addLast(track.id)
        while (recentlyPlayed.size > 25) recentlyPlayed.removeFirst()

        player.setMediaItem(MediaItem.fromUri(track.streamUrl))
        player.prepare()
        player.play()
        applyTempo()
        onTrackStarted(MusicSource.AUDIUS, track.id, track.title, track.artist, track.bpm)
    }

    private fun playNextAudius() {
        val queue = audiusQueue
        val next = if (rhythmSyncEnabled) {
            pickTrackForPace(queue) ?: pickTrackForPace(bpmPool)
        } else null
        val fallback = queue.indexOfFirst { it.id == currentAudiusTrack?.id }
            .let { idx -> queue.getOrNull((idx + 1).mod(queue.size.coerceAtLeast(1))) }
        (next ?: fallback)?.let { playAudius(it, queue) }
    }

    private fun playPreviousAudius() {
        if (player.currentPosition > 5_000) {
            player.seekTo(0)
            return
        }
        val queue = audiusQueue
        val idx = queue.indexOfFirst { it.id == currentAudiusTrack?.id }
        if (idx > 0) playAudius(queue[idx - 1], queue) else player.seekTo(0)
    }

    // ================================================================= RADIO

    fun playStation(station: RadioStation) {
        stopSpotifyPolling()
        currentSource = MusicSource.RADIO
        activeStation = station
        currentAudiusTrack = null
        title = station.name
        artist = "${station.genre} • ${station.targetBpm} BPM"
        coverUrl = null
        trackBpm = station.targetBpm
        durationMs = 0f
        progressMs = 0f
        errorMessage = null
        hasMedia = true
        setRate(1f)
        player.setMediaItem(MediaItem.fromUri(station.streamUrl))
        player.prepare()
        player.play()
        onTrackStarted(MusicSource.RADIO, station.id, station.name, station.genre, station.targetBpm)
    }

    // ================================================================= SPOTIFY

    fun spotifyLoginUri(): Uri = spotify.buildAuthorizeUri()

    suspend fun handleSpotifyRedirect(uri: Uri): String? {
        val err = spotify.handleRedirect(uri)
        if (err == null) onSpotifyConnected()
        return err
    }

    private fun onSpotifyConnected() {
        spotifyConnected = true
        scope.launch { spotifyPlaylists = spotify.playlists() }
    }

    fun disconnectSpotify() {
        spotify.disconnect()
        spotifyConnected = false
        spotifyPlaylists = emptyList()
        if (currentSource == MusicSource.SPOTIFY) {
            stopSpotifyPolling()
            currentSource = MusicSource.AUDIUS
            isPlaying = false
            hasMedia = false
        }
    }

    suspend fun searchSpotify(query: String): List<SpotifyTrack> = spotify.searchTracks(query)

    fun playSpotifyTrack(track: SpotifyTrack) = playOnSpotify(listOf(track.uri), null, track.uri)

    fun playSpotifyPlaylist(playlist: SpotifyPlaylist) = playOnSpotify(emptyList(), playlist.uri, playlist.uri)

    private fun playOnSpotify(uris: List<String>, contextUri: String?, openUri: String) {
        player.pause()
        currentSource = MusicSource.SPOTIFY
        hasMedia = true
        setRate(1f)
        lastAppliedTier = paceTier
        scope.launch {
            when (spotify.play(uris, contextUri)) {
                SpotifyPlayResult.PLAYING -> { errorMessage = null; startSpotifyPolling() }
                SpotifyPlayResult.NO_DEVICE -> {
                    errorMessage = "Abre Spotify en tu teléfono una vez para activarlo como dispositivo."
                    openInSpotifyApp(openUri)
                    startSpotifyPolling()
                }
                SpotifyPlayResult.PREMIUM_REQUIRED -> {
                    errorMessage = "Spotify sólo permite controlar la reproducción con Premium. Abriendo en la app…"
                    openInSpotifyApp(openUri)
                    startSpotifyPolling()
                }
                SpotifyPlayResult.NOT_CONNECTED -> {
                    spotifyConnected = false
                    errorMessage = "La sesión de Spotify expiró. Vuelve a conectar."
                }
                SpotifyPlayResult.ERROR -> errorMessage = "Spotify no respondió. Inténtalo de nuevo."
            }
        }
    }

    private fun openInSpotifyApp(uri: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { errorMessage = "Instala la app de Spotify para reproducir esta música." }
    }

    private fun startSpotifyPolling() {
        spotifyPollJob?.cancel()
        spotifyPollJob = scope.launch {
            var lastTitle = ""
            while (isActive && currentSource == MusicSource.SPOTIFY) {
                spotify.nowPlaying()?.let { np ->
                    isPlaying = np.isPlaying
                    title = np.title
                    artist = np.artist
                    coverUrl = np.imageUrl
                    progressMs = np.progressMs.toFloat()
                    durationMs = np.durationMs.toFloat()
                    trackBpm = null
                    if (np.title != lastTitle) {
                        lastTitle = np.title
                        onTrackStarted(MusicSource.SPOTIFY, np.title, np.title, np.artist, null)
                    }
                }
                delay(if (isPlaying) 3_000 else 8_000)
            }
        }
    }

    private fun stopSpotifyPolling() {
        spotifyPollJob?.cancel()
        spotifyPollJob = null
    }

    /** Si la Web API falla (cuenta gratuita), se envía la tecla multimedia a la sesión activa. */
    private fun mediaKey(keyCode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    // ================================================================= CONTROLES

    fun togglePlayPause() {
        when (currentSource) {
            MusicSource.SPOTIFY -> {
                val wasPlaying = isPlaying
                isPlaying = !wasPlaying
                scope.launch {
                    val ok = if (wasPlaying) spotify.pause() else spotify.resume()
                    if (!ok) mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                }
            }
            else -> {
                if (!hasMedia) { startPaceMix(); return }
                if (player.isPlaying) player.pause()
                else {
                    if (player.playbackState == Player.STATE_IDLE) player.prepare()
                    player.play()
                }
            }
        }
    }

    fun next() {
        when (currentSource) {
            MusicSource.AUDIUS -> playNextAudius()
            MusicSource.RADIO -> {
                val idx = stations.indexOf(activeStation)
                playStation(stations[(idx + 1).mod(stations.size)])
            }
            MusicSource.SPOTIFY -> scope.launch { if (!spotify.next()) mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) }
        }
    }

    fun previous() {
        when (currentSource) {
            MusicSource.AUDIUS -> playPreviousAudius()
            MusicSource.RADIO -> {
                val idx = stations.indexOf(activeStation)
                playStation(stations[(idx - 1).mod(stations.size)])
            }
            MusicSource.SPOTIFY -> scope.launch { if (!spotify.previous()) mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
        }
    }

    fun seekTo(fraction: Float) {
        if (currentSource == MusicSource.AUDIUS && player.duration > 0) {
            player.seekTo((player.duration * fraction.coerceIn(0f, 1f)).toLong())
        }
    }

    /** Baja el volumen mientras habla la audio-guía. */
    fun duck(enabled: Boolean) {
        player.volume = if (enabled) 0.25f else 1f
    }

    fun release() {
        tickJob?.cancel()
        stopSpotifyPolling()
        player.release()
    }
}
