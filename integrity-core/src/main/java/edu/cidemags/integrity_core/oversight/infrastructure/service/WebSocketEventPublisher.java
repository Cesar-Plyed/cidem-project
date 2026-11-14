package edu.cidemags.integrity_core.oversight.infrastructure.service;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Publica eventos de STOMP a los suscriptores de Oversight.
 * Implements spec 15: WebSocketEventPublisher.
 */
@Service
public class WebSocketEventPublisher {

    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketEventPublisher(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publica un evento a /topic/oversight/events.
     *
     * @param eventType tipo del evento (TAB_SWITCH, ALERT, etc.)
     * @param payload   datos del evento en formato JSON
     */
    public void publish(String eventType, Map<String, Object> payload) {
        Map<String, Object> event = Map.of("eventType", eventType, "payload", payload);
        messagingTemplate.convertAndSend("/topic/oversight/events", (Object) event);
    }

    /**
     * Publica un evento a un cliente específico por session id.
     */
    public void publishToSession(String sessionId, String eventType, Map<String, Object> payload) {
        Map<String, Object> event = Map.of("sessionId", sessionId, "eventType", eventType, "payload", payload);
        messagingTemplate.convertAndSend("/topic/oversight/events", (Object) event);
    }
}
