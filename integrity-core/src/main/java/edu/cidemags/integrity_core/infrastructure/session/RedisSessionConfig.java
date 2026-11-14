package edu.cidemags.integrity_core.infrastructure.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuración de Redis para sesiones de examen.
 * <p>
 * Define TTLs por tipo de sesión y habilita {@code @Scheduled} para autosave.
 * </p>
 */
@Configuration
@EnableScheduling
public class RedisSessionConfig {

    /** TTL para heartbeat de sesión: 15 s. */
    @Value("${session.heartbeat-ttl:15s}")
    private Duration heartbeatTtl;

    /** TTL para sesión activa: 30 min. */
    @Value("${session.active-ttl:30m}")
    private Duration activeTtl;

    /** TTL para autosave: 30 s. */
    @Value("${session.autosave-interval:30s}")
    private Duration autosaveInterval;

    @Bean
    public StringRedisTemplate redisSessionTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    public Duration getHeartbeatTtl() { return heartbeatTtl; }
    public Duration getActiveTtl() { return activeTtl; }
    public Duration getAutosaveInterval() { return autosaveInterval; }
}
