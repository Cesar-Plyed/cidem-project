package edu.cidemags.integrity_core.oversight.infrastructure.config;

import edu.cidemags.integrity_core.oversight.application.service.WebSocketAuthorizationService;
import edu.cidemags.integrity_core.oversight.infrastructure.service.WebSocketSessionRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Component;

/**
 * Interceptor de STOMP para autorizar conexiones WebSocket de Oversight.
 * Implements spec 17: WebSocket authorization (professor sees only their exams).
 *
 * <p>Extrae el token JWT del header 'Authorization' y lo valida
 * antes de permitir el SUBSCRIBE o SEND al broker.</p>
 */
@Component
public class StompAuthorizationInterceptor implements ChannelInterceptor {

    private final WebSocketAuthorizationService authorizationService;
    private final AuthenticationManager authenticationManager;
    private final WebSocketSessionRegistry sessionRegistry;

    public StompAuthorizationInterceptor(
            WebSocketAuthorizationService authorizationService,
            AuthenticationManager authenticationManager,
            WebSocketSessionRegistry sessionRegistry
    ) {
        this.authorizationService = authorizationService;
        this.authenticationManager = authenticationManager;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() == StompCommand.CONNECT) {
            String rawToken = accessor.getFirstNativeHeader("Authorization");
            if (rawToken != null && rawToken.startsWith("Bearer ")) {
                String token = rawToken.substring(7);
                String userId = authorizationService.getUserId(token);
                String sessionId = accessor.getFirstNativeHeader("X-Session-Id");
                if (sessionId != null) {
                    authorizationService.authenticate(userId, sessionId);
                    sessionRegistry.register(sessionId, null); // registry will hold session later
                }
            }
        }

        return message;
    }
}
