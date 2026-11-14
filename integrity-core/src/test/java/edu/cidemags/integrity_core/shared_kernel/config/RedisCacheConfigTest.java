package edu.cidemags.integrity_core.shared_kernel.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración: valida que {@link CacheConfig} crea los cachés
 * definidos y que la serialización JSON en Redis funciona correctamente.
 */
@Testcontainers
@SpringBootTest(properties = {
        "cache.idempotency-ttl=1m",
        "cache.exam-ttl=2m",
        "cache.session-ttl=5m",
        "cache.alert-ttl=10m"
})
class RedisCacheConfigTest {

    @Container
    @ServiceConnection
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    // El contexto completo necesita un datasource: sin Postgres, Flyway/Hibernate
    // no arrancan y el test fallaba por "Failed to determine a suitable driver".
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void caches_exist_for_allDomains() {
        assertThat(cacheManager.getCache("idempotency")).isNotNull();
        assertThat(cacheManager.getCache("exams")).isNotNull();
        assertThat(cacheManager.getCache("sessions")).isNotNull();
        assertThat(cacheManager.getCache("alerts")).isNotNull();
    }

    @Test
    void putAndGet_roundTripsCorrectly() {
        Cache cache = cacheManager.getCache("idempotency");
        assertThat(cache).isNotNull();

        cache.put("test-key", "test-value");
        Cache.ValueWrapper val = cache.get("test-key");
        assertThat(val).isNotNull();
        assertThat(val.get()).isEqualTo("test-value");
    }

    @Test
    void redisTemplate_canStoreAndRetrieve() {
        redisTemplate.opsForValue().set("cidem:cache:test", "hello");
        String value = redisTemplate.opsForValue().get("cidem:cache:test");
        assertThat(value).isEqualTo("hello");
    }

    @Test
    void redisTemplate_delete_removesValue() {
        redisTemplate.opsForValue().set("cidem:cache:remove", "bye");
        redisTemplate.delete("cidem:cache:remove");
        String value = redisTemplate.opsForValue().get("cidem:cache:remove");
        assertThat(value).isNull();
    }
}
