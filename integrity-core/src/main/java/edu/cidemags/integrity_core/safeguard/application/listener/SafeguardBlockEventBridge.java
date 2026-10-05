package edu.cidemags.integrity_core.safeguard.application.listener;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import edu.cidemags.integrity_core.safeguard.domain.event.SafeguardDomainEvent;
import edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent;

/**
 * Puente entre el evento interno de dominio y el contrato compartido
 * {@link SafeguardBlockEvent} (ToDo.md, Sprint 1).
 * <p>
 * Solo se convierte {@code RiskEventRecorded} (un riesgo registrado/bloqueo): los demás
 * eventos internos (inicio, cambio de estado, cierre) no son "bloqueos" y no se
 * reenvían a EVALUATOR/OVERSIGHT en este sprint. El {@code eventId} del contrato es el
 * UUID del evento de riesgo, único por (session, event) en BD, de modo que los
 * consumidores pueden deduplicar.
 */
@Component
public class SafeguardBlockEventBridge {

    private final ApplicationEventPublisher publisher;

    public SafeguardBlockEventBridge(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void onRiskEventRecorded(SafeguardDomainEvent.RiskEventRecorded event) {
        publisher.publishEvent(new SafeguardBlockEvent(
                event.eventId(),
                event.examId(),
                event.attemptId(),
                event.sessionId(),
                event.studentId(),
                event.occurredAt(),
                SafeguardBlockEvent.SOURCE,
                Map.of(
                        "eventType", event.eventType().name(),
                        "severity", event.severity(),
                        "riskPoints", event.riskPoints(),
                        "totalRiskScore", event.totalRiskScore())));
    }
}
