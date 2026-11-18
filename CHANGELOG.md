# Registro de Cambios (CHANGELOG)

## [2.2.1] - 2026-11-18
### Corregido
- La app vuelve a compilar: comillas sin escapar en `GpxExporter`, `PoiCategory` duplicado, tipos inexistentes en `PoiSearch` y argumentos de `GpsFix` en `WalkSimulator`.
- Pruebas unitarias de GPX y del formateo imperial corregidas (114 pruebas en verde).

### Añadido
- APK descargable desde GitHub Releases y sección "Descargar" en el README.

## [2.2.0] - 2026-11-18
### Añadido
- Soporte multilingüe completo (español e inglés) para turistas internacionales.
- Flujo automatizado de CI/CD con GitHub Actions para compilación y pruebas.
- Documentación integral de arquitectura (`docs/ARCHITECTURE.md`) y referencias de API.
- Cola de sincronización offline (`SyncQueue`) para registrar caminatas sin conexión.
- Selector inteligente de listas de reproducción de Spotify según cadencia.
- Transiciones suaves de audio con algoritmo de igual potencia (`CrossfadeMath`).
- Tokens semánticos de diseño y retroalimentación háptica táctil en giros.

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
