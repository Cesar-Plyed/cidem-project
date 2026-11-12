package edu.cidemags.integrity_core.sentinel.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra {@link SentinelProperties}. El {@code StringRedisTemplate} que
 * consumen los contadores lo aporta la autoconfiguracion de Spring Boot
 * ({@code spring-boot-starter-data-redis}), por eso no se redefine aqui.
 */
@Configuration
@EnableConfigurationProperties(SentinelProperties.class)
public class RedisConfig {
}
