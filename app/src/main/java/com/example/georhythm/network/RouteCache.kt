package com.example.georhythm.network

import com.example.georhythm.data.RouteGeometry

class RouteCache(
    private val maxEntries: Int = 20,
    private val ttlMs: Long = 600_000L
) {
    private val cache = LinkedHashMap<RouteCacheKey, CachedRoute>(maxEntries, 0.75f, true)

    @Synchronized
    fun get(key: RouteCacheKey, nowMs: Long = System.currentTimeMillis()): RouteGeometry? {
        val entry = cache[key] ?: return null
        if (nowMs - entry.timestampMs > ttlMs) {
            cache.remove(key)
            return null
        }
        return entry.geometry
    }

    @Synchronized
    fun put(key: RouteCacheKey, geometry: RouteGeometry, nowMs: Long = System.currentTimeMillis()) {
        if (cache.size >= maxEntries) {
            val oldest = cache.keys.firstOrNull()
            if (oldest != null) cache.remove(oldest)
        }
        cache[key] = CachedRoute(geometry, nowMs)
    }

    @Synchronized
    fun size(): Int = cache.size

    @Synchronized
    fun clear() = cache.clear()
}
