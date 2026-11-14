package edu.cidemags.integrity_core.shared_kernel.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración de {@link CacheConfig} contra un Redis real
 * (Testcontainers): cachés de dominio, TTLs y serialización JSON.
 *
 * <p>No arranca el contexto de Spring a propósito. Antes era un
 * {@code @SpringBootTest}, lo que obligaba a levantar también Postgres/Flyway y
 * competía con los contenedores de otras clases de test; en CI el
 * <em>round-trip</em> de caché falló de forma intermitente sin que el código de
 * {@code CacheConfig} tuviera nada que ver. Aquí se instancia la configuración
 * tal cual y se conecta solo a Redis.</p>
 */
@Testcontainers
class RedisCacheConfigTest {

    private static final String CACHE_NAME = "idempotency";
    private static final Duration VISIBILITY_TIMEOUT = Duration.ofSeconds(2);

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(standalone);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        CacheConfig config = configWithTtls(
                Duration.ofMinutes(1), Duration.ofMinutes(2),
                Duration.ofMinutes(5), Duration.ofMinutes(10));
        cacheManager = config.cacheManager(connectionFactory);
        ((RedisCacheManager) cacheManager).afterPropertiesSet();

        connectionFactory.getConnection().serverCommands().flushDb();
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    private static CacheConfig configWithTtls(Duration idempotency, Duration exam,
                                              Duration session, Duration alert) {
        CacheConfig config = new CacheConfig();
        ReflectionTestUtils.setField(config, "idempotencyTtl", idempotency);
        ReflectionTestUtils.setField(config, "examTtl", exam);
        ReflectionTestUtils.setField(config, "sessionTtl", session);
        ReflectionTestUtils.setField(config, "alertTtl", alert);
        return config;
    }

    private static Duration ttlOf(Map<String, RedisCacheConfiguration> configs, String cacheName) {
        RedisCacheConfiguration cacheConfig = configs.get(cacheName);
        assertThat(cacheConfig).as("configuración de la caché %s", cacheName).isNotNull();
        return cacheConfig.getTtlFunction().getTimeToLive(cacheName, "value");
    }

    /** Redis puede tardar unos milisegundos en hacer visible una escritura. */
    private static Cache.ValueWrapper awaitValue(Cache cache, Object key) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        Cache.ValueWrapper value = cache.get(key);
        while (value == null && System.nanoTime() < deadline) {
            Thread.sleep(25);
            value = cache.get(key);
        }
        return value;
    }

    @Test
    void caches_exist_for_allDomains() {
        assertThat(cacheManager.getCache(CACHE_NAME)).isNotNull();
        assertThat(cacheManager.getCache("exams")).isNotNull();
        assertThat(cacheManager.getCache("sessions")).isNotNull();
        assertThat(cacheManager.getCache("alerts")).isNotNull();
    }

    @Test
    void ttl_isTheConfiguredOnePerDomain() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

        Map<String, RedisCacheConfiguration> configs =
                ((RedisCacheManager) cacheManager).getCacheConfigurations();

        assertThat(ttlOf(configs, CACHE_NAME)).isEqualTo(Duration.ofMinutes(1));
        assertThat(ttlOf(configs, "exams")).isEqualTo(Duration.ofMinutes(2));
        assertThat(ttlOf(configs, "sessions")).isEqualTo(Duration.ofMinutes(5));
        assertThat(ttlOf(configs, "alerts")).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void putAndGet_roundTripsCorrectly() throws InterruptedException {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();

        cache.put("test-key", "test-value");

        Cache.ValueWrapper value = awaitValue(cache, "test-key");
        assertThat(value).isNotNull();
        assertThat(value.get()).isEqualTo("test-value");
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
