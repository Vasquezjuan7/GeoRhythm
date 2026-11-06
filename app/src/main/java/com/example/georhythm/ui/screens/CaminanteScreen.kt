package com.example.georhythm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.georhythm.data.FavoritePlace
import com.example.georhythm.data.UserManager
import com.example.georhythm.data.WalkRecord
import com.example.georhythm.ui.auth.AVATARS
import com.example.georhythm.ui.auth.RUNNER_TYPES
import com.example.georhythm.ui.components.*
import com.example.georhythm.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CaminanteScreen(
    userManager: UserManager,
    speedKmh: Float,
    cadenceSpm: Int,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToFavorite: (FavoritePlace) -> Unit,
    onCreateAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val c = LocalGeoColors.current
    val scope = rememberCoroutineScope()
    val user = userManager.profile
    val stats = userManager.stats
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { userManager.refresh() }

    LazyColumn(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader("Caminante", "Tu historia caminando por Pasto") {
                IconButton(onClick = onToggleTheme) {
                    Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, "Cambiar tema", tint = c.text)
                }
            }
        }

        // Perfil
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surface)
                    .border(1.dp, c.border, RoundedCornerShape(24.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(62.dp).clip(CircleShape).background(c.accentGradient),
                    contentAlignment = Alignment.Center
                ) { Text(user?.avatarEmoji ?: "👟", fontSize = 30.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.name ?: "Caminante", color = c.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(user?.runnerType ?: "", color = Emerald, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(user?.email ?: "", color = c.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    user?.let {
                        Text(
                            "Desde " + SimpleDateFormat("d MMM yyyy", Locale("es", "CO")).format(Date(it.registeredAt)),
                            color = c.textMuted, fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, "Editar perfil", tint = c.textMuted) }
            }
        }

        // Estado de sincronización
        item {
            if (userManager.isGuest) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gold.copy(alpha = 0.12f))
                        .clickable(onClick = onCreateAccount).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudOff, null, tint = Gold)
                    Spacer(Modifier.width(10.dp))
                    Text("Modo invitado: tus datos sólo están en este teléfono. Crea una cuenta para guardarlos en la nube.",
                        color = c.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, null, tint = Emerald, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (userManager.isSyncing) "Sincronizando…" else "Guardado en la nube (Supabase)", color = c.textMuted, fontSize = 12.sp)
                }
            }
        }

        // Ahora mismo
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Velocidad actual", "${speedKmh.roundToInt()} km/h", NavBlue, Icons.Default.Speed, Modifier.weight(1f))
                StatTile("Cadencia", if (cadenceSpm > 0) "$cadenceSpm spm" else "—", Mint, Icons.Default.GraphicEq, Modifier.weight(1f))
            }
        }

        // Totales
        item {
            Text("Tus totales", color = c.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Kilómetros", String.format(Locale.US, "%.1f", stats.totalKm), Emerald, Icons.Default.Route, Modifier.weight(1f))
                StatTile("Caminatas", "${stats.totalWalks}", Violet, Icons.AutoMirrored.Filled.DirectionsWalk, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Minutos", "${stats.totalMinutes}", Gold, Icons.Default.Timer, Modifier.weight(1f))
                StatTile("Lugares descubiertos", "${stats.discoveredPois}", Color(0xFFDB2777), Icons.Default.Place, Modifier.weight(1f))
            }
        }

        // Favoritos
        if (userManager.favorites.isNotEmpty()) {
            item { Text("Lugares favoritos", color = c.text, style = MaterialTheme.typography.titleMedium) }
            items(userManager.favorites, key = { it.id }) { f ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface)
                        .clickable { onNavigateToFavorite(f) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(f.name, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, "Ir", tint = Emerald, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Historial
        item { Text("Últimas caminatas", color = c.text, style = MaterialTheme.typography.titleMedium) }
        if (userManager.recentWalks.isEmpty()) {
            item {
                Text("Aún no tienes caminatas. Inicia una ruta: al llegar se guarda automáticamente.",
                    color = c.textMuted, fontSize = 13.sp)
            }
        } else {
            items(userManager.recentWalks) { w -> WalkRow(w) }
        }

        item {
            OutlinedButton(
                onClick = { scope.launch { userManager.logout(); onLogout() } },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }

    if (editing && user != null) {
        EditProfileDialog(
            initialName = user.name, initialType = user.runnerType, initialAvatar = user.avatarEmoji,
            onDismiss = { editing = false },
            onSave = { n, t, a -> scope.launch { userManager.updateProfile(n, t, a) }; editing = false }
        )
    }
}

@Composable
private fun WalkRow(w: WalkRecord) {
    val c = LocalGeoColors.current
    val date = SimpleDateFormat("d MMM · HH:mm", Locale("es", "CO")).format(Date(w.startedAt))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Emerald.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = Emerald, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(w.destinationName ?: "Caminata libre", color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(date, color = c.textMuted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDistanceText((w.distanceMeters / 1000).toFloat()), color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${w.durationSeconds / 60} min · ${w.avgCadence} spm", color = c.textMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialType: String,
    initialAvatar: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableStateOf(initialType) }
    var avatar by remember { mutableStateOf(initialAvatar) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AVATARS.forEach { a ->
                        Text(a, fontSize = 22.sp, modifier = Modifier.clip(CircleShape)
                            .background(if (a == avatar) Emerald.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { avatar = a }.padding(6.dp))
                    }
                }
                RUNNER_TYPES.forEach { t ->
                    Row(Modifier.fillMaxWidth().clickable { type = t }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = t == type, onClick = { type = t })
                        Text(t)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim().ifBlank { initialName }, type, avatar) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
