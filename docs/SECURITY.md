# Política de Seguridad y Manejo de Claves

## Buenas Prácticas
1. **Secretos locales:** Claves como `SPOTIFY_CLIENT_ID`, `SUPABASE_PUBLISHABLE_KEY` y `MAPTILER_API_KEY` se almacenan en `local.properties` y nunca se suben al repositorio.
2. **Row Level Security (RLS):** Toda tabla en Supabase restringe las operaciones de lectura y escritura al usuario autenticado (`auth.uid() = user_id`).
3. **Flujo PKCE:** La autenticación de Spotify implementa Proof Key for Code Exchange sin exponer claves secretas en la app cliente.
