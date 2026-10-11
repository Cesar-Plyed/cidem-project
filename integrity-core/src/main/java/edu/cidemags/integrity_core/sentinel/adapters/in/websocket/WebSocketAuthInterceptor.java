package edu.cidemags.integrity_core.sentinel.adapters.in.websocket;

import edu.cidemags.integrity_core.auth.domain.service.TokenProvider;
import edu.cidemags.integrity_core.auth.domain.service.TokenProvider.AccessTokenClaims;
import edu.cidemags.integrity_core.sentinel.ports.in.ExamOversightAccessPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Pattern OVERSIGHT_TOPIC =
            Pattern.compile("^/topic/oversight/exams/([0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12})$");
    private final TokenProvider tokenProvider;
    private final ExamOversightAccessPort examOversightAccessPort;

    public WebSocketAuthInterceptor(TokenProvider tokenProvider,
            ExamOversightAccessPort examOversightAccessPort) {
        this.tokenProvider = tokenProvider;
        this.examOversightAccessPort = examOversightAccessPort;
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

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && accessor.getDestination() != null
                && accessor.getDestination().startsWith("/topic/oversight/")) {
            authorizeOversightSubscription(accessor);
        }

        return message;
    }

    private void authorizeOversightSubscription(StompHeaderAccessor accessor) {
        Matcher matcher = OVERSIGHT_TOPIC.matcher(accessor.getDestination());
        if (!matcher.matches() || !(accessor.getUser() instanceof SentinelPrincipal principal)) {
            throw new AccessDeniedException("Destino de supervisión no válido");
        }
        if ("ADMINISTRATIVO".equals(principal.role())) {
            return;
        }
        if (!"PROFESOR".equals(principal.role())
                || !examOversightAccessPort.canManageExam(UUID.fromString(matcher.group(1)), principal.userId())) {
            throw new AccessDeniedException("No puedes supervisar este examen");
        }
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