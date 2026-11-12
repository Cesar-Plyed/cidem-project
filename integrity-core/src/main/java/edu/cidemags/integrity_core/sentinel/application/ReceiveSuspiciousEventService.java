package edu.cidemags.integrity_core.sentinel.application;

import edu.cidemags.integrity_core.sentinel.application.service.SentinelEventPublisher;
import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.AlertEscalationPolicy;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventCommand;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventUseCase;
import edu.cidemags.integrity_core.sentinel.ports.out.AlertRepositoryPort;
import edu.cidemags.integrity_core.sentinel.ports.out.EventWindowPort;
import edu.cidemags.integrity_core.sentinel.ports.out.SuspiciousEventRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReceiveSuspiciousEventService implements ReceiveSuspiciousEventUseCase {

    private static final Duration ESCALATION_WINDOW = Duration.ofMinutes(2);

    private final SuspiciousEventRepositoryPort eventRepository;
    private final EventWindowPort eventWindowPort;
    private final AlertRepositoryPort alertRepository;
    private final AlertEscalationPolicy escalationPolicy;
    private final SentinelEventPublisher eventPublisher;

    public ReceiveSuspiciousEventService(
        SuspiciousEventRepositoryPort eventRepository,
        EventWindowPort eventWindowPort,
        AlertRepositoryPort alertRepository,
        AlertEscalationPolicy escalationPolicy,
        SentinelEventPublisher eventPublisher
    ) {
        this.eventRepository = eventRepository;
        this.eventWindowPort = eventWindowPort;
        this.alertRepository = alertRepository;
        this.escalationPolicy = escalationPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<Alert> receive(ReceiveSuspiciousEventCommand command) {
        ExamSessionId examSessionId = new ExamSessionId(command.examSessionId());
        StudentId studentId = new StudentId(command.studentId());
        AttemptId attemptId = new AttemptId(command.attemptId());

        SuspiciousEvent newEvent = new SuspiciousEvent(
            UUID.randomUUID(), examSessionId, studentId, attemptId,
            command.type(), command.occurredAt(), command.examId(),
            command.source() == null ? "SENTINEL" : command.source(),
            command.payload()
        );

        eventRepository.save(newEvent);
        eventWindowPort.record(newEvent);

        List<SuspiciousEvent> recentEvents = eventWindowPort.findRecentEvents(
            examSessionId, studentId, command.type(), ESCALATION_WINDOW
        );

        Optional<Alert> generatedAlert = escalationPolicy.evaluate(recentEvents);

        generatedAlert.ifPresent(alert -> {
            alertRepository.save(alert);
            eventPublisher.publish(alert);
        });

        return generatedAlert;
    }
}
