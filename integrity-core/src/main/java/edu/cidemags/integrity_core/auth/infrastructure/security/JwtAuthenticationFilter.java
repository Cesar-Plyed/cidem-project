package edu.cidemags.integrity_core.auth.infrastructure.security;

import edu.cidemags.integrity_core.auth.domain.exception.InvalidAccessTokenException;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/countries",
            "/auth/refresh",
            "/auth/logout"
    );

    private final TokenProvider tokenProvider;

    public JwtAuthenticationFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getServletPath();
        boolean isPublicPath = PUBLIC_PATHS.stream().anyMatch(requestPath::startsWith);

        String header = request.getHeader(AUTH_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            TokenProvider.AccessTokenClaims claims = tokenProvider.validateAccessToken(token);

            var authority = new SimpleGrantedAuthority("ROLE_" + claims.role().name());
            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.userId(), null, List.of(authority));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidAccessTokenException ex) {
            if (isPublicPath) {
                filterChain.doFilter(request, response);
                return;
            }
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + ex.getMessage() + "\"}");
        }
    }
}