package edu.cidemags.integrity_core.sentinel.adapters.in.websocket;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventCommand;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventUseCase;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

@Controller
public class SentinelWebSocketController {

    private final ReceiveSuspiciousEventUseCase receiveSuspiciousEventUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    public SentinelWebSocketController(
        ReceiveSuspiciousEventUseCase receiveSuspiciousEventUseCase,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.receiveSuspiciousEventUseCase = receiveSuspiciousEventUseCase;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/sentinel/events")
    public void receiveEvent(SuspiciousEventPayload payload, Principal principal) {
        ReceiveSuspiciousEventCommand command = new ReceiveSuspiciousEventCommand(
            payload.examSessionId(),
            payload.studentId(),
            payload.type(),
            payload.occurredAt()
        );

        Optional<Alert> alert = receiveSuspiciousEventUseCase.receive(command);

        alert.ifPresent(a -> messagingTemplate.convertAndSendToUser(
            principal.getName(),
            "/queue/sentinel/alerts",
            AlertNotification.from(a)
        ));
    }
}