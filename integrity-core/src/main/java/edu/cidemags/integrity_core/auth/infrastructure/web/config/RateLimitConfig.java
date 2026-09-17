package edu.cidemags.integrity_core.auth.infrastructure.web.config;

import edu.cidemags.integrity_core.auth.infrastructure.web.ratelimit.InMemoryRateLimiter;
import edu.cidemags.integrity_core.auth.infrastructure.web.ratelimit.RateLimitingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(
            InMemoryRateLimiter rateLimiter) {
        RateLimitingFilter filter = new RateLimitingFilter(rateLimiter);

        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/auth/login", "/auth/register");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}