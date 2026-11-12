package edu.cidemags.integrity_core.sentinel.adapters.in.websocket;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.ports.in.AttemptOwnershipPort;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventCommand;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventUseCase;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Controller
public class SentinelWebSocketController {

    private final ReceiveSuspiciousEventUseCase receiveSuspiciousEventUseCase;
    private final SimpMessagingTemplate messagingTemplate;
    private final AttemptOwnershipPort attemptOwnershipPort;

    public SentinelWebSocketController(
        ReceiveSuspiciousEventUseCase receiveSuspiciousEventUseCase,
        SimpMessagingTemplate messagingTemplate,
        AttemptOwnershipPort attemptOwnershipPort
    ) {
        this.receiveSuspiciousEventUseCase = receiveSuspiciousEventUseCase;
        this.messagingTemplate = messagingTemplate;
        this.attemptOwnershipPort = attemptOwnershipPort;
    }

    /**
     * Único ingreso de eventos de Sentinel (STOMP /ws/sentinel).
     *
     * Hardening: los ids del payload no son de confianza y se contrastan con el
     * principal del JWT (construido por WebSocketAuthInterceptor en el CONNECT):
     * - studentId SIEMPRE se toma del principal; el del payload se ignora.
     * - attemptId debe existir y pertenecer al alumno autenticado; si no, el
     *   evento se descarta silenciosamente (un alumno no puede atribuir
     *   eventos a un intento ajeno).
     */
    @MessageMapping("/sentinel/events")
    public void receiveEvent(SuspiciousEventPayload payload, Principal principal) {
        UUID principalUserId = parsePrincipal(principal);
        if (principalUserId == null || payload.attemptId() == null) {
            return;
        }

        var owner = attemptOwnershipPort.findOwner(payload.attemptId())
            .filter(candidate -> candidate.studentId().equals(principalUserId));
        if (owner.isEmpty()) {
            return;
        }

        ReceiveSuspiciousEventCommand command = new ReceiveSuspiciousEventCommand(
            payload.examSessionId(),
            principalUserId,
            payload.attemptId(),
            payload.type(),
            payload.occurredAt(),
            owner.get().examId()
        );

        Optional<Alert> alert = receiveSuspiciousEventUseCase.receive(command);

        alert.ifPresent(a -> messagingTemplate.convertAndSendToUser(
            principal.getName(),
            "/queue/sentinel/alerts",
            AlertNotification.from(a)
        ));
    }

    private static UUID parsePrincipal(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
