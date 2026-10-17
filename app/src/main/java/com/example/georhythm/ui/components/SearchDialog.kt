package com.example.georhythm.ui.components

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.georhythm.data.FavoritePlace
import com.example.georhythm.data.LocalData
import com.example.georhythm.data.NavigationDestination
import com.example.georhythm.network.NominatimService
import com.example.georhythm.ui.theme.Emerald
import com.example.georhythm.ui.theme.Gold
import com.example.georhythm.ui.theme.LocalGeoColors
import kotlinx.coroutines.delay

/** Buscador a pantalla completa: lugares destacados, favoritos y búsqueda en OpenStreetMap. */
@Composable
fun SearchDestinationDialog(
    currentLocation: Location?,
    favorites: List<FavoritePlace>,
    onDismiss: () -> Unit,
    onSelectDestination: (NavigationDestination) -> Unit
) {
    val c = LocalGeoColors.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<NavigationDestination>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    // Búsqueda con retardo de 600 ms (respeta el límite de 1 petición/s de Nominatim)
    LaunchedEffect(query) {
        if (query.trim().length < 3) { results = emptyList(); isLoading = false; return@LaunchedEffect }
        isLoading = true
        delay(600)
        results = NominatimService.searchPlaces(query)
        isLoading = false
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val localMatches = remember(query) {
        val q = query.trim()
        val base = if (q.isEmpty()) LocalData.pois.filter { !it.id.startsWith("x") }.take(8)
        else LocalData.pois.filter { it.name.contains(q, true) || it.category.label.contains(q, true) }
        base.map { NavigationDestination(it.name, it.address, it.latLng) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(c.background).statusBarsPadding().imePadding()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = c.text) }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    placeholder = { Text("¿A dónde quieres caminar?", color = c.textMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        when {
                            isLoading -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Emerald)
                            query.isNotEmpty() -> IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Limpiar", tint = c.textMuted) }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { results.firstOrNull()?.let(onSelectDestination) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald, unfocusedBorderColor = c.border,
                        focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                        focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = Emerald
                    )
                )
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                if (query.isEmpty() && favorites.isNotEmpty()) {
                    item { SectionLabel("Tus favoritos") }
                    items(favorites) { f ->
                        ResultRow(NavigationDestination(f.name, "Guardado", f.latLng), currentLocation, Icons.Default.Star, Gold, onSelectDestination)
                    }
                }
                if (localMatches.isNotEmpty()) {
                    item { SectionLabel(if (query.isEmpty()) "Lugares destacados de Pasto" else "En GeoRhythm") }
                    items(localMatches) { d -> ResultRow(d, currentLocation, Icons.Default.Place, Emerald, onSelectDestination) }
                }
                if (results.isNotEmpty()) {
                    item { SectionLabel("Resultados en el mapa") }
                    items(results) { d -> ResultRow(d, currentLocation, Icons.Default.Search, c.textMuted, onSelectDestination) }
                }
                if (!isLoading && query.trim().length >= 3 && results.isEmpty() && localMatches.isEmpty()) {
                    item {
                        Text(
                            "No encontramos \"$query\". Prueba con una dirección (ej. \"Calle 18 Carrera 27\") o un barrio.",
                            color = c.textMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), color = LocalGeoColors.current.textMuted, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun ResultRow(
    d: NavigationDestination,
    loc: Location?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: (NavigationDestination) -> Unit
) {
    val c = LocalGeoColors.current
    val km = calculateDistanceKm(loc, d.latLng)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onClick(d) }.padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(d.category, color = c.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (km != null) Text(formatDistanceText(km), color = c.textMuted, fontSize = 12.sp)
    }
}
