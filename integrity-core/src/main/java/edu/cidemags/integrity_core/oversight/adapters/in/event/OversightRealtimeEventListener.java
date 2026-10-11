package edu.cidemags.integrity_core.oversight.adapters.in.event;

import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.sentinel.application.event.AlertGeneratedEvent;
import edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent;
import edu.cidemags.integrity_core.shared.events.ExamAttemptRevokedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OversightRealtimeEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public OversightRealtimeEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener
    public void onSafeguardEvent(SafeguardBlockEvent event) {
        var payload = event.payload();
        publish(new OversightRealtimeEvent(
                event.examId(),
                event.attemptId(),
                event.studentId(),
                LogSource.SAFEGUARD,
                severity(payload == null ? null : payload.get("severity")),
                eventType(payload == null ? null : payload.get("eventType")),
                event.occurredAt()));
    }

    @TransactionalEventListener
    public void onSentinelAlert(AlertGeneratedEvent event) {
        var alert = event.alert();
        publish(new OversightRealtimeEvent(
                alert.examSessionId().value(),
                alert.attemptId().value(),
                alert.studentId().value(),
                LogSource.SENTINEL,
                alert.severity(),
                alert.reason(),
                alert.generatedAt()));
    }

    @TransactionalEventListener
    public void onAttemptRevoked(ExamAttemptRevokedEvent event) {
        publish(new OversightRealtimeEvent(
                event.examId(),
                event.attemptId(),
                event.studentId(),
                LogSource.EVALUATOR,
                edu.cidemags.integrity_core.sentinel.domain.AlertSeverity.HIGH,
                "Intento revocado por integridad académica",
                event.occurredAt()));
    }

    private void publish(OversightRealtimeEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/oversight/exams/" + event.examId(),
                event);
    }

    private static edu.cidemags.integrity_core.sentinel.domain.AlertSeverity severity(Object raw) {
        int value = raw instanceof Number number ? number.intValue() : 1;
        if (value >= 3) return edu.cidemags.integrity_core.sentinel.domain.AlertSeverity.HIGH;
        if (value == 2) return edu.cidemags.integrity_core.sentinel.domain.AlertSeverity.MEDIUM;
        return edu.cidemags.integrity_core.sentinel.domain.AlertSeverity.LOW;
    }

    private static String eventType(Object raw) {
        return raw == null ? "Evento de SafeGuard" : raw.toString();
    }
}
