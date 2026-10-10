package com.example.georhythm.data

import android.content.Context
import com.example.georhythm.BuildConfig
import com.example.georhythm.network.Http
import com.example.georhythm.network.HttpResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Sesión autenticada de Supabase persistida en SharedPreferences. */
data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtSec: Long,
    val userId: String,
    val email: String
)

sealed class AuthOutcome {
    data class Success(val session: SupabaseSession) : AuthOutcome()
    /** Registro correcto, pero el proyecto exige confirmar el correo antes de iniciar sesión. */
    data object NeedsEmailConfirmation : AuthOutcome()
    data class Failure(val message: String) : AuthOutcome()
}

/**
 * Cliente REST ligero para Supabase (GoTrue Auth + PostgREST), sin dependencias extra.
 * Todas las tablas están protegidas por Row Level Security: cada usuario sólo ve sus datos.
 */
class SupabaseClient(context: Context) {

    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val apiKey = BuildConfig.SUPABASE_KEY
    private val prefs = context.getSharedPreferences("georhythm_supabase", Context.MODE_PRIVATE)
    private val refreshMutex = Mutex()

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    var session: SupabaseSession? = loadSession()
        private set

    // ---------------------------------------------------------------- AUTH

    suspend fun signUp(email: String, password: String, metadata: JSONObject): AuthOutcome {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("data", metadata)
        // El enlace del correo de confirmación abre la app (georhythm://auth-callback) e inicia sesión
        val redirect = java.net.URLEncoder.encode(AUTH_REDIRECT, "UTF-8")
        val res = Http.request("$baseUrl/auth/v1/signup?redirect_to=$redirect", "POST", authHeaders(null), body.toString())
        if (!res.ok) return AuthOutcome.Failure(translateAuthError(res))
        val json = JSONObject(res.body)
        // Si el proyecto requiere confirmación de correo no se devuelve access_token
        return if (json.has("access_token")) {
            AuthOutcome.Success(storeSession(json))
        } else {
            AuthOutcome.NeedsEmailConfirmation
        }
    }

    suspend fun signIn(email: String, password: String): AuthOutcome {
        val body = JSONObject().put("email", email).put("password", password)
        val res = Http.request(
            "$baseUrl/auth/v1/token?grant_type=password", "POST", authHeaders(null), body.toString()
        )
        if (!res.ok) return AuthOutcome.Failure(translateAuthError(res))
        return AuthOutcome.Success(storeSession(JSONObject(res.body)))
    }

    /** Procesa georhythm://auth-callback#access_token=...&refresh_token=... (correo de confirmación). */
    suspend fun signInFromRedirect(fragment: String): AuthOutcome {
        val params = fragment.split("&").mapNotNull {
            val i = it.indexOf('=')
            if (i > 0) it.substring(0, i) to java.net.URLDecoder.decode(it.substring(i + 1), "UTF-8") else null
        }.toMap()
        params["error_description"]?.let { return AuthOutcome.Failure(it.replace('+', ' ')) }
        val access = params["access_token"] ?: return AuthOutcome.Failure("El enlace no es válido o ya expiró.")
        val userRes = Http.request("$baseUrl/auth/v1/user", "GET", authHeaders(access))
        if (!userRes.ok) return AuthOutcome.Failure("El enlace ya expiró. Inicia sesión con tu correo y contraseña.")
        val json = JSONObject()
            .put("access_token", access)
            .put("refresh_token", params["refresh_token"] ?: "")
            .put("expires_in", params["expires_in"]?.toLongOrNull() ?: 3600)
            .put("user", JSONObject(userRes.body))
        params["expires_at"]?.toLongOrNull()?.let { json.put("expires_at", it) }
        return AuthOutcome.Success(storeSession(json))
    }

    suspend fun signOut() {
        val token = session?.accessToken
        clearSession()
        if (token != null) {
            Http.request("$baseUrl/auth/v1/logout", "POST", authHeaders(token), "{}")
        }
    }

    /** Devuelve un access token válido, refrescándolo si está a punto de expirar. */
    private suspend fun validToken(): String? = refreshMutex.withLock {
        val s = session ?: return null
        val now = System.currentTimeMillis() / 1000
        if (s.expiresAtSec - now > 60) return s.accessToken
        val body = JSONObject().put("refresh_token", s.refreshToken)
        val res = Http.request(
            "$baseUrl/auth/v1/token?grant_type=refresh_token", "POST", authHeaders(null), body.toString()
        )
        return when {
            res.ok -> storeSession(JSONObject(res.body)).accessToken
            res.code in 400..499 -> { clearSession(); null } // refresh token revocado
            else -> s.accessToken // sin red: reintentar más tarde
        }
    }

    // ---------------------------------------------------------------- REST (PostgREST)

    /** SELECT: [query] es la parte después de `?`, p. ej. "select=*&order=started_at.desc". */
    suspend fun select(table: String, query: String): JSONArray? {
        val token = validToken() ?: return null
        val res = Http.request("$baseUrl/rest/v1/$table?$query", "GET", authHeaders(token))
        return if (res.ok) runCatching { JSONArray(res.body) }.getOrNull() else null
    }

    /** INSERT (o UPSERT si [upsert] = true, usando la clave primaria / restricción única). */
    suspend fun insert(table: String, row: JSONObject, upsert: Boolean = false, onConflict: String? = null): Boolean {
        val token = validToken() ?: return false
        val prefer = if (upsert) "return=minimal,resolution=merge-duplicates" else "return=minimal"
        val url = buildString {
            append("$baseUrl/rest/v1/$table")
            if (onConflict != null) append("?on_conflict=$onConflict")
        }
        val res = Http.request(url, "POST", authHeaders(token) + ("Prefer" to prefer), row.toString())
        return res.ok
    }

    suspend fun update(table: String, filter: String, patch: JSONObject): Boolean {
        val token = validToken() ?: return false
        val res = Http.request(
            "$baseUrl/rest/v1/$table?$filter", "PATCH",
            authHeaders(token) + ("Prefer" to "return=minimal"), patch.toString()
        )
        return res.ok
    }

    suspend fun delete(table: String, filter: String): Boolean {
        val token = validToken() ?: return false
        return Http.request("$baseUrl/rest/v1/$table?$filter", "DELETE", authHeaders(token)).ok
    }

    // ---------------------------------------------------------------- INTERNOS

    private fun authHeaders(accessToken: String?): Map<String, String> {
        val h = mutableMapOf("apikey" to apiKey)
        if (accessToken != null) h["Authorization"] = "Bearer $accessToken"
        return h
    }

    private fun storeSession(json: JSONObject): SupabaseSession {
        val user = json.optJSONObject("user")
        val expiresIn = json.optLong("expires_in", 3600)
        val expiresAt = json.optLong("expires_at", System.currentTimeMillis() / 1000 + expiresIn)
        val s = SupabaseSession(
            accessToken = json.getString("access_token"),
            refreshToken = json.optString("refresh_token"),
            expiresAtSec = expiresAt,
            userId = user?.optString("id") ?: session?.userId.orEmpty(),
            email = user?.optString("email") ?: session?.email.orEmpty()
        )
        prefs.edit()
            .putString("access_token", s.accessToken)
            .putString("refresh_token", s.refreshToken)
            .putLong("expires_at", s.expiresAtSec)
            .putString("user_id", s.userId)
            .putString("email", s.email)
            .apply()
        session = s
        return s
    }

    private fun loadSession(): SupabaseSession? {
        val access = prefs.getString("access_token", null) ?: return null
        return SupabaseSession(
            accessToken = access,
            refreshToken = prefs.getString("refresh_token", "") ?: "",
            expiresAtSec = prefs.getLong("expires_at", 0),
            userId = prefs.getString("user_id", "") ?: "",
            email = prefs.getString("email", "") ?: ""
        )
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
        session = null
    }

    private fun translateAuthError(res: HttpResult): String {
        if (res.code == -1) return "Sin conexión a internet. Revisa tu red e inténtalo de nuevo."
        val json = runCatching { JSONObject(res.body) }.getOrNull()
        val code = json?.optString("error_code").orEmpty().ifBlank { json?.optString("code").orEmpty() }
        val msg = json?.optString("msg").orEmpty()
            .ifBlank { json?.optString("error_description").orEmpty() }
            .ifBlank { json?.optString("message").orEmpty() }
        return when {
            code == "invalid_credentials" || msg.contains("Invalid login", true) -> "Correo o contraseña incorrectos."
            code == "email_not_confirmed" -> "Confirma tu correo (revisa tu bandeja de entrada) antes de iniciar sesión."
            code == "user_already_exists" || msg.contains("already registered", true) -> "Ya existe una cuenta con ese correo."
            code == "weak_password" || msg.contains("Password should", true) -> "La contraseña debe tener al menos 6 caracteres."
            code == "validation_failed" || msg.contains("invalid format", true) -> "El correo no tiene un formato válido."
            code == "over_email_send_rate_limit" || res.code == 429 -> "Demasiados intentos. Espera un momento."
            msg.isNotBlank() -> msg
            else -> "Error del servidor (${res.code})."
        }
    }

    companion object {
        const val AUTH_REDIRECT = "georhythm://auth-callback"
    }
}
