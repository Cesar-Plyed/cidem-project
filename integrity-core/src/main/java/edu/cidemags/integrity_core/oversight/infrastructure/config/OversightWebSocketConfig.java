package edu.cidemags.integrity_core.oversight.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.cidemags.integrity_core.oversight.application.service.WebSocketAuthorizationService;
import edu.cidemags.integrity_core.oversight.infrastructure.service.WebSocketSessionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * Configura el WebSocket de Oversight con STOMP.
 * Implements spec 15: Spring WebSocket config (/ws/oversight).
 */
@Configuration
@EnableWebSocketMessageBroker
public class OversightWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<MessageConverter> messageConverters;

    private final WebSocketSessionRegistry sessionRegistry;
    private final WebSocketAuthorizationService authzService;
    private final AuthenticationManager authManager;

    public OversightWebSocketConfig(
            ObjectMapper objectMapper,
            WebSocketSessionRegistry sessionRegistry,
            WebSocketAuthorizationService authzService,
            AuthenticationManager authManager
    ) {
        this.messageConverters = List.of(createJacksonMessageConverter(objectMapper));
        this.sessionRegistry = sessionRegistry;
        this.authzService = authzService;
        this.authManager = authManager;
    }

    @Bean
    public StompAuthorizationInterceptor stompAuthorizationInterceptor() {
        return new StompAuthorizationInterceptor(authzService, authManager, sessionRegistry);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic/oversight");
        registry.setApplicationDestinationPrefixes("/app/oversight");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/oversight")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthorizationInterceptor());
    }

    private MessageConverter createJacksonMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT));
        return converter;
    }
}
