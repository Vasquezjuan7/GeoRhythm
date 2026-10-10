package com.example.georhythm.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.georhythm.data.AuthOutcome
import com.example.georhythm.data.UserManager
import com.example.georhythm.ui.theme.*
import kotlinx.coroutines.launch

val AVATARS = listOf("👟", "🚶", "🏃", "🧭", "🎧", "🌋")
val RUNNER_TYPES = listOf("Explorador Urbano", "Paseante Casual", "Caminante Rápido", "Corredor")

@Composable
fun AuthScreen(userManager: UserManager, onAuthSuccess: () -> Unit) {
    val c = LocalGeoColors.current
    val scope = rememberCoroutineScope()
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var avatar by remember { mutableStateOf(AVATARS[0]) }
    var runnerType by remember { mutableStateOf(RUNNER_TYPES[0]) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun validate(): String? = when {
        isRegister && name.isBlank() -> "Escribe tu nombre."
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Escribe un correo válido."
        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
        else -> null
    }

    fun submit() {
        error = validate()
        info = null
        if (error != null) return
        if (!userManager.supabase.isConfigured) {
            error = "Falta configurar SUPABASE_URL en local.properties."
            return
        }
        loading = true
        scope.launch {
            val out = if (isRegister) userManager.register(name, email, password, runnerType, avatar)
            else userManager.login(email, password)
            loading = false
            when (out) {
                is AuthOutcome.Success -> onAuthSuccess()
                AuthOutcome.NeedsEmailConfirmation -> {
                    info = "¡Cuenta creada! Te enviamos un correo para confirmarla. Luego inicia sesión."
                    isRegister = false
                    password = ""
                }
                is AuthOutcome.Failure -> error = out.message
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF052E2B), c.background, c.background))
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().imePadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(c.accentGradient),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.GraphicEq, null, tint = c.onAccent, modifier = Modifier.size(38.dp)) }
            Spacer(Modifier.height(16.dp))
            Text("GeoRhythm", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Camina Pasto al ritmo de tu música", color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.surface)
                    .border(1.dp, c.border, RoundedCornerShape(26.dp)).padding(20.dp)
            ) {
                // Selector Entrar / Crear cuenta
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surfaceAlt).padding(4.dp)) {
                    listOf(false to "Entrar", true to "Crear cuenta").forEach { (reg, label) ->
                        Text(
                            label, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = if (isRegister == reg) c.onAccent else c.textMuted,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (isRegister == reg) Emerald else Color.Transparent)
                                .clickable { isRegister = reg; error = null }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                if (isRegister) {
                    AuthField(name, { name = it }, "Nombre", Icons.Default.Person)
                    Spacer(Modifier.height(10.dp))
                }
                AuthField(email, { email = it }, "Correo electrónico", Icons.Default.Email, KeyboardType.Email)
                Spacer(Modifier.height(10.dp))
                AuthField(
                    password, { password = it }, "Contraseña", Icons.Default.Lock, KeyboardType.Password,
                    visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                "Mostrar contraseña", tint = c.textMuted)
                        }
                    }
                )

                if (isRegister) {
                    Spacer(Modifier.height(14.dp))
                    Text("Tu avatar", color = c.textMuted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        AVATARS.forEach { a ->
                            Box(
                                Modifier.size(42.dp).clip(CircleShape)
                                    .background(if (a == avatar) Emerald.copy(alpha = 0.25f) else c.surfaceAlt)
                                    .border(1.5.dp, if (a == avatar) Emerald else Color.Transparent, CircleShape)
                                    .clickable { avatar = a },
                                contentAlignment = Alignment.Center
                            ) { Text(a, fontSize = 20.sp) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("¿Cómo caminas?", color = c.textMuted, fontSize = 12.sp)
                    FlowRowChips(RUNNER_TYPES, runnerType) { runnerType = it }
                }

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Danger, fontSize = 13.sp)
                }
                info?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Emerald, fontSize = 13.sp)
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { submit() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = c.onAccent)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = c.onAccent, strokeWidth = 2.dp)
                    else Text(if (isRegister) "Crear cuenta" else "Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { userManager.loginAsGuest(); onAuthSuccess() }) {
                Text("Continuar como invitado", color = Color.White.copy(alpha = 0.8f))
            }
            Text(
                "Como invitado tus datos sólo se guardan en este teléfono.",
                color = c.textMuted, fontSize = 11.sp, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null
) {
    val c = LocalGeoColors.current
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = c.textMuted) },
        trailingIcon = trailing,
        singleLine = true,
        visualTransformation = visual,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald, unfocusedBorderColor = c.border,
            focusedLabelColor = Emerald, unfocusedLabelColor = c.textMuted,
            focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = Emerald
        )
    )
}

@Composable
private fun FlowRowChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { opt ->
                    FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt, fontSize = 12.sp) })
                }
            }
        }
    }
}
