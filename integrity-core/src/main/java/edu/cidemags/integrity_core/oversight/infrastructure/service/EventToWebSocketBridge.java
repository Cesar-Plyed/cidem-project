package edu.cidemags.integrity_core.oversight.infrastructure.service;

import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conecta los eventos de dominio (LogEntry, Alert, SuspiciousEvent) con el WebSocket.
 * Implements spec 16: EventToWebSocketBridge + WebSocketSessionRegistry.
 */
@Service
public class EventToWebSocketBridge {

    private static final Logger log = LoggerFactory.getLogger(EventToWebSocketBridge.class);

    private final WebSocketEventPublisher eventPublisher;

    public EventToWebSocketBridge(WebSocketEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Conecta un evento de SuspiciousEvent al WebSocket.
     */
    public void bridge(SuspiciousEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", event.type().name());
        payload.put("attemptId", event.attemptId().value().toString());
        payload.put("studentId", event.studentId().value().toString());
        payload.put("timestamp", event.occurredAt().toString());
        payload.put("payload", event.payload());

        eventPublisher.publish("SUSPICIOUS_EVENT", payload);
        log.debug("Bridged SuspiciousEvent: type={}, attemptId={}", event.type(), event.attemptId());
    }

    /**
     * Conecta una Alerta al WebSocket.
     */
    public void bridge(Alert alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("severity", alert.severity().name());
        payload.put("attemptId", alert.attemptId().value().toString());
        payload.put("studentId", alert.studentId().value().toString());
        payload.put("timestamp", alert.generatedAt().toString());
        payload.put("reason", alert.reason());
        payload.put("alertType", alert.alertType());

        eventPublisher.publish("ALERT", payload);
        log.debug("Bridged Alert: severity={}, attemptId={}", alert.severity(), alert.attemptId());
    }

    /**
     * Conecta un LogEntry al WebSocket.
     */
    public void bridge(LogEntry entry) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", entry.source().name());
        payload.put("description", entry.description());
        payload.put("studentId", entry.studentId().value().toString());
        payload.put("attemptId", entry.attemptId().value().toString());
        payload.put("timestamp", entry.occurredAt().toString());
        payload.put("severity", entry.severity().name());

        eventPublisher.publish("LOG_ENTRY", payload);
        log.debug("Bridged LogEntry: source={}, description={}", entry.source(), entry.description());
    }
}
