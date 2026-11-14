# Referencia de APIs y Servicios Externos

## 1. OSRM (Open Source Routing Machine) FOSSGIS
- **URL Base:** `https://routing.openstreetmap.de/routed-foot/route/v1/foot/`
- **Perfil:** Peatonal estricto (senderos, andenes y pasajes peatonales de Pasto).

## 2. Audius Web API
- **Endpoint:** `v1/tracks/trending`
- **Campos utilizados:** `id`, `title`, `bpm`, `duration`, `streamUrl`.

## 3. Supabase Backend
- **PostgREST:** `/rest/v1/walk_sessions`, `/rest/v1/discovered_places`.
- **Seguridad:** Autenticación JWT + Row Level Security (RLS).
