package edu.cidemags.integrity_core.oversight.infrastructure.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registra y gestiona las sesiones WebSocket activas.
 * Implements spec 16: WebSocketSessionRegistry.
 */
@Service
public class WebSocketSessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Registra una nueva sesión WebSocket.
     *
     * @param sessionId id único de la sesión
     * @param session   la sesión real
     */
    public void register(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
    }

    /**
     * Elimina una sesión WebSocket.
     *
     * @param sessionId id único de la sesión
     */
    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Devuelve todas las sesiones activas.
     */
    public List<WebSocketSession> getAllSessions() {
        return List.copyOf(sessions.values());
    }

    /**
     * Devuelve el número de sesiones activas.
     */
    public int activeCount() {
        return sessions.size();
    }

    /**
     * Devuelve si una sesión está activa.
     */
    public boolean isActive(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}
