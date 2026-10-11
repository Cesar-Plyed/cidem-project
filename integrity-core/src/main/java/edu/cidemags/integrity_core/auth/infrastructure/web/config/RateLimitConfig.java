package edu.cidemags.integrity_core.auth.infrastructure.web.config;

import edu.cidemags.integrity_core.auth.infrastructure.web.ratelimit.InMemoryRateLimiter;
import edu.cidemags.integrity_core.auth.infrastructure.web.ratelimit.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(
            InMemoryRateLimiter rateLimiter,
            // Solo true detrás de un proxy que sobrescriba X-Forwarded-For;
            // en cualquier otro escenario habilitarlo deja evadir el limite.
            @Value("${security.rate-limit.trust-proxy-header:false}") boolean trustProxyHeader) {
        RateLimitingFilter filter = new RateLimitingFilter(rateLimiter, trustProxyHeader);

        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        // /auth/refresh tambien es publico: sin limite es un endpoint de fuerza bruta.
        registration.addUrlPatterns("/auth/login", "/auth/register", "/auth/refresh");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}