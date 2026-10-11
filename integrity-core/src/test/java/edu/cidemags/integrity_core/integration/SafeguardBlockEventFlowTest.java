package edu.cidemags.integrity_core.integration;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import edu.cidemags.integrity_core.evaluator.infrastructure.events.SafeguardBlockEventListener;
import edu.cidemags.integrity_core.safeguard.application.listener.SafeguardBlockEventBridge;
import edu.cidemags.integrity_core.safeguard.domain.event.SafeguardDomainEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;
import edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Prueba de publicación/recepción del contrato SafeguardBlockEvent (ToDo.md, Sprint 1):
 * RiskEventRecorded (interno) → puente → SafeguardBlockEvent (compartido) → listener
 * de EVALUATOR, dentro de un contexto Spring mínimo con el mismo cableado por
 * @EventListener que la aplicación real.
 */
class SafeguardBlockEventFlowTest {

    @Test
    void riskEventRecorded_reachesEvaluatorListenerWithStandardContractFields() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(SafeguardBlockEventBridge.class, SafeguardBlockEventListener.class);
            ctx.refresh();

            UUID sessionId = UUID.randomUUID();
            UUID examId = UUID.randomUUID();
            UUID attemptId = UUID.randomUUID();
            UUID studentId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            Instant occurredAt = Instant.now();

            ctx.publishEvent(new SafeguardDomainEvent.RiskEventRecorded(
                    sessionId, examId, attemptId, studentId, eventId,
                    SafeguardEventType.TAB_SWITCH, 2, 10, 30, occurredAt));

            SafeguardBlockEvent received = ctx.getBean(SafeguardBlockEventListener.class).getLastReceived();
            assertNotNull(received, "el listener de EVALUATOR debe recibir el evento");
            assertEquals(eventId, received.eventId());
            assertEquals(examId, received.examId());
            assertEquals(attemptId, received.attemptId());
            assertEquals(sessionId, received.examSessionId(), "examSessionId = sesión de SafeGuard");
            assertEquals(studentId, received.studentId());
            assertEquals(occurredAt, received.occurredAt());
            assertEquals(SafeguardBlockEvent.SOURCE_SAFEGUARD, received.source());
            assertEquals("safeguard", received.source());
            assertEquals("TAB_SWITCH", received.payload().get("eventType"));
            assertEquals(2, received.payload().get("severity"));
            assertEquals(10, received.payload().get("riskPoints"));
            assertEquals(30, received.payload().get("totalRiskScore"));
        }
    }

    @Test
    void nonRiskEvents_areNotForwardedAsBlocks() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(SafeguardBlockEventBridge.class, SafeguardBlockEventListener.class);
            ctx.refresh();

            ctx.publishEvent(new SafeguardDomainEvent.SessionStarted(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), Instant.now()));

            assertNull(ctx.getBean(SafeguardBlockEventListener.class).getLastReceived(),
                    "solo los riesgos registrados se convierten en SafeguardBlockEvent");
        }
    }
}
