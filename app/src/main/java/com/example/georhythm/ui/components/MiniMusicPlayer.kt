package com.example.georhythm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.georhythm.audio.MusicPlayerEngine
import com.example.georhythm.audio.MusicSource
import com.example.georhythm.ui.theme.Emerald
import com.example.georhythm.ui.theme.LocalGeoColors
import com.example.georhythm.ui.theme.Mint
import com.example.georhythm.ui.theme.SpotifyGreen
import com.example.georhythm.ui.theme.Violet

/** Mini reproductor flotante del mapa. Muestra el ajuste de tempo cuando "Ritmo al paso" está activo. */
@Composable
fun MiniMusicPlayer(
    player: MusicPlayerEngine,
    onOpenMusicLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalGeoColors.current
    val accent = when (player.currentSource) {
        MusicSource.SPOTIFY -> SpotifyGreen
        MusicSource.RADIO -> Emerald
        MusicSource.AUDIUS -> Violet
    }
    GlassCard(modifier = modifier.padding(horizontal = 12.dp), corner = 20.dp, onClick = onOpenMusicLibrary) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.musicGradient),
                contentAlignment = Alignment.Center
            ) {
                if (player.coverUrl != null) {
                    AsyncImage(player.coverUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(player.title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (player.rhythmSyncEnabled && player.currentSource == MusicSource.AUDIUS && player.trackBpm != null && player.cadenceSpm > 0) {
                        Text(
                            "${player.trackBpm} → ${(player.trackBpm!! * player.playbackRate).toInt()} BPM · ",
                            color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Text(player.artist, color = c.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = { player.previous() }, Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipPrevious, "Anterior", tint = c.text)
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) {
                if (player.isBuffering) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { player.togglePlayPause() }) {
                        Icon(
                            if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (player.isPlaying) "Pausar" else "Reproducir", tint = Color.White
                        )
                    }
                }
            }
            IconButton(onClick = { player.next() }, Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipNext, "Siguiente", tint = c.text)
            }
        }
        if (player.durationMs > 0f) {
            LinearProgressIndicator(
                progress = { (player.progressMs / player.durationMs).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = accent, trackColor = Color.Transparent
            )
        }
    }
}
