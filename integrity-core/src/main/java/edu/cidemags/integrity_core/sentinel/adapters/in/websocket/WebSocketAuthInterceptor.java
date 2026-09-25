package edu.cidemags.integrity_core.sentinel.adapters.in.websocket;

import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider.AccessTokenClaims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final TokenProvider tokenProvider;

    public WebSocketAuthInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = firstHeader(accessor, "Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Falta el header Authorization con formato 'Bearer <token>'");
            }

            String token = authHeader.substring("Bearer ".length());
            AccessTokenClaims claims = tokenProvider.validateAccessToken(token);

            accessor.setUser(new SentinelPrincipal(claims.userId(), claims.email(), claims.role().name()));
        }

        return message;
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    public record SentinelPrincipal(UUID userId, String email, String role) implements Principal {
        @Override
        public String getName() {
            return userId.toString();
        }
    }
}