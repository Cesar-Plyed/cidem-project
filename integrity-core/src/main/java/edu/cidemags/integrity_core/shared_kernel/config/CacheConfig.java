package edu.cidemags.integrity_core.shared_kernel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de caché distribuida con Redis.
 * <p>
 * Define nombres de caché, TTLs y serialización para las cachés del dominio
 * (exams, sessions, alerts, config).
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.idempotency-ttl:5m}")
    private Duration idempotencyTtl;

    @Value("${cache.exam-ttl:10m}")
    private Duration examTtl;

    @Value("${cache.session-ttl:30m}")
    private Duration sessionTtl;

    @Value("${cache.alert-ttl:1h}")
    private Duration alertTtl;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("idempotency",
                buildConfig(idempotencyTtl));
        cacheConfigurations.put("exams",
                buildConfig(examTtl));
        cacheConfigurations.put("sessions",
                buildConfig(sessionTtl));
        cacheConfigurations.put("alerts",
                buildConfig(alertTtl));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(buildConfig(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private RedisCacheConfiguration buildConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
