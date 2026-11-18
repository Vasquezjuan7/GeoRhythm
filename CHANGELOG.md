# Registro de Cambios (CHANGELOG)

## [2.1.0] - 2026-11-12
### Añadido
- Resiliencia de red con clase sellada `NetworkResult` y política de reintentos exponenciales.
- Exportación estándar en formato GPX 1.1 con metadatos de cadencia y waypoints patrimoniales.
- Cálculo de métricas metabólicas (METs) y gasto calórico según peso y velocidad de marcha.
- Búsqueda tolerante a acentos y filtros temáticos de lugares de Pasto.
- Repositorio de preferencias de usuario (sistema métrico/imperial y audio-guía).
- Avisos auditivos enriquecidos (`AudioCue`) para giros y splits por kilómetro.
- Caché LRU de geometrías OSRM con TTL para optimizar consumo de datos.
- Perfil de elevación y cálculo de pendientes topográficas en Pasto.

## [2.0.0] - 2026-11-05
### Añadido
- Sincronización con base de datos Supabase y Row Level Security.
- Navegación peatonal con OSRM FOSSGIS.
- Servicio en primer plano para caminata con pantalla apagada.
- Adaptación de tempo musical a la cadencia de pasos.

## [1.0.0] - 2026-10-15
### Añadido
- Versión inicial con mapa vectorial MapLibre GL y tema Sonic Journey.
