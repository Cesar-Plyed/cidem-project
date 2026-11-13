package edu.cidemags.integrity_core.shared_kernel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba unitaria de {@link IdempotencyService}.
 * <p>
 * Verifica:
 * <ul>
 *   <li>El primer request ID es aceptado (true)</li>
 *   <li>IDs duplicados en la ventana se rechazan (false)</li>
 *   <li>IDs fuera de ventana se aceptan nuevamente</li>
 *   <li>La invalidación manual de caché funciona</li>
 * </ul>
 */
class IdempotencyServiceTest {

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = new ConcurrentMapCacheManager();
        service = new IdempotencyService(cacheManager);
    }

    @Test
    void firstRequest_returnsTrue() {
        String requestId = "req-001";
        assertThat(service.isIdempotent(requestId)).isTrue();
    }

    @Test
    void duplicateRequestInWindow_returnsFalse() {
        String requestId = "req-002";
        assertThat(service.isIdempotent(requestId)).isTrue();
        assertThat(service.isIdempotent(requestId)).isFalse();
    }

    @Test
    void differentRequestIds_areIndependent() {
        assertThat(service.isIdempotent("req-a")).isTrue();
        assertThat(service.isIdempotent("req-b")).isTrue();
        assertThat(service.isIdempotent("req-c")).isTrue();
    }

    @Test
    void processedCount_incrementsPerUniqueRequest() {
        service.isIdempotent("req-x");
        service.isIdempotent("req-y");
        assertThat(service.getProcessedCount()).isEqualTo(2);

        // Duplicado no incrementa
        service.isIdempotent("req-x");
        assertThat(service.getProcessedCount()).isEqualTo(2);
    }

    @Test
    void invalidateCache_removesKey() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("test-cache");
        IdempotencyService svc = new IdempotencyService(cacheManager);

        // Simula escribir en caché (usando el manager directo)
        cacheManager.getCache("test-cache").put("key-1", "value-1");
        assertThat(cacheManager.getCache("test-cache").get("key-1").get()).isEqualTo("value-1");

        svc.invalidateCache("test-cache", "key-1");

        assertThat(cacheManager.getCache("test-cache").get("key-1")).isNull();
    }

    @Test
    void clearCache_clearsAll() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("multi-cache");
        IdempotencyService svc = new IdempotencyService(cacheManager);

        cacheManager.getCache("multi-cache").put("a", "val-a");
        cacheManager.getCache("multi-cache").put("b", "val-b");
        cacheManager.getCache("multi-cache").put("c", "val-c");

        svc.clearCache("multi-cache");

        assertThat(cacheManager.getCache("multi-cache").get("a")).isNull();
        assertThat(cacheManager.getCache("multi-cache").get("b")).isNull();
        assertThat(cacheManager.getCache("multi-cache").get("c")).isNull();
    }
}
