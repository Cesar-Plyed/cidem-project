package edu.cidemags.integrity_core.auth.infrastructure.web.ratelimit;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final InMemoryRateLimiter rateLimiter;
    private final boolean trustProxyHeader;

    /**
     * @param trustProxyHeader si es true se toma el cliente real del header
     *        X-Forwarded-For. Solo debe activarse detrás de un proxy que
     *        sobrescriba el header: si no, cualquier cliente puede rotar el
     *        header y evadir el limite por completo.
     */
    public RateLimitingFilter(InMemoryRateLimiter rateLimiter, boolean trustProxyHeader) {
        this.rateLimiter = rateLimiter;
        this.trustProxyHeader = trustProxyHeader;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

        String key = resolveClientIp(request) + ":" + request.getRequestURI();

        if (!rateLimiter.tryConsume(key)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Demasiados intentos. Intenta de nuevo mas tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustProxyHeader) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
