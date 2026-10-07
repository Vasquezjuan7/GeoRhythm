# GeoRhythm 🎧🚶

**Sonic Journey Navigation**: app Android para caminar San Juan de Pasto (Nariño, Colombia) al ritmo de tu música. La app mide tu velocidad con el GPS, calcula tu cadencia de pasos y adapta la música en tiempo real, mientras te guía a pie por rutas históricas con audio-guía.

## Funciones

- **Mapa vectorial 3D** (MapLibre + MapTiler) con modo día/noche, capas (calles, satélite, relieve) y lugares de Pasto con coordenadas verificadas en OpenStreetMap.
- **Navegación peatonal paso a paso** con el servidor OSRM de FOSSGIS (perfil a pie real), recálculo automático si te desvías y detección de llegada.
- **Rutas recomendadas** por el centro histórico, con paradas en orden y audio-guía (texto a voz) en cada parada.
- **Ritmo al paso**: la música acelera o desacelera (sin cambiar el tono) para igualar tu cadencia, y la siguiente canción se elige por BPM.
  - **Audius**: búsqueda y reproducción de canciones completas con BPM real (ExoPlayer).
  - **Radio**: emisoras en vivo; cambia a la que mejor se ajusta a tu paso.
  - **Spotify**: login PKCE, búsqueda, playlists y cambio de playlist según tu ritmo (controlar la reproducción requiere Premium).
- **Cuentas y estadísticas en Supabase**: perfil, caminatas, lugares descubiertos, favoritos e historial musical, protegidos con Row Level Security. También hay modo invitado (solo local).
- **Caminata con la pantalla apagada**: servicio en primer plano (ubicación + reproducción) con notificación y controles.

## Arquitectura

```
app/src/main/java/com/example/georhythm/
├── GeoRhythmApp.kt          # Application: crea el controlador a nivel de proceso
├── MainActivity.kt          # UI (Jetpack Compose), single-activity
├── core/
│   ├── GeoRhythmController.kt  # GPS, navegación, reproductor, audio-guía, guardado
│   └── WalkService.kt          # Servicio en primer plano (pantalla apagada)
├── audio/MusicPlayerEngine.kt  # ExoPlayer + Spotify + lógica de ritmo
├── data/                    # Modelos, datos locales, Supabase, WalkTracker
├── network/                 # Http, Audius, Spotify, OSRM, Nominatim
└── ui/                      # theme, components, screens, auth
supabase/migrations/         # Esquema SQL de la base de datos
```

## Configuración

1. **Requisitos:** Android Studio (JDK incluido) y Android SDK 37.
2. **Claves:** crea o edita `local.properties` (no se sube al repositorio):
   ```properties
   MAPTILER_API_KEY=tu_clave_de_maptiler
   SUPABASE_URL=https://TU_PROYECTO.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
   SPOTIFY_CLIENT_ID=tu_client_id
   ```
3. **Base de datos:** en Supabase → SQL Editor, ejecuta `supabase/migrations/20261105000000_init_georhythm_schema.sql`.
   - Para no requerir confirmación por correo: Authentication → Sign In / Providers → Email → desactivar *Confirm email*.
   - Si mantienes la confirmación: Authentication → URL Configuration → Site URL = `georhythm://auth-callback`.
4. **Spotify:** en developer.spotify.com/dashboard → tu app → Settings, agrega el Redirect URI `georhythm://callback`, marca *Web API* y añade tu cuenta en *User Management*.
5. **Compilar:**
   ```bash
   ./gradlew :app:assembleDebug
   ```

## Créditos de datos y servicios

Mapas © MapTiler © OpenStreetMap contributors · Rutas: OSRM / FOSSGIS · Búsqueda: Nominatim · Música: Audius, Spotify Web API.
