package edu.cidemags.integrity_core.shared_kernel.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de idempotencia para endpoints que pueden recibir el mismo request múltiples veces.
 * <p>
 * Usa un {@code ConcurrentHashMap} en memoria para rastrear request IDs en una ventana
 * de tiempo configurable. También expone invalidación manual de caché por key.
 * </p>
 *
 * @see CacheConfig
 */
@Service
public class IdempotencyService {

    private static final long DEFAULT_WINDOW_MS = 300_000L; // 5 min

    private final CacheManager cacheManager;
    private final ConcurrentHashMap<String, Long> requestIds = new ConcurrentHashMap<>();
    private final AtomicLong processedCount = new AtomicLong(0);

    public IdempotencyService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Verifica si un request ID ya fue procesado en la ventana actual.
     * Devuelve {@code true} si el ID es nuevo (aceptado), {@code false} si ya existe.
     */
    public boolean isIdempotent(String requestId) {
        long now = System.currentTimeMillis();
        long windowStart = now - DEFAULT_WINDOW_MS;

        // Limpieza periódica: elimina entradas viejas
        requestIds.entrySet().removeIf(entry -> entry.getValue() < windowStart);

        Long existing = requestIds.putIfAbsent(requestId, now);
        if (existing != null) {
            return false; // ya procesado
        }
        processedCount.incrementAndGet();
        return true;
    }

    /**
     * Invalida manualmente una key de caché.
     */
    public void invalidateCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    /**
     * Invalida toda la caché de un nombre.
     */
    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public long getProcessedCount() {
        return processedCount.get();
    }

    public long getWindowMs() {
        return DEFAULT_WINDOW_MS;
    }
}
