package edu.cidemags.integrity_core.sentinel.application;

import edu.cidemags.integrity_core.sentinel.application.event.AlertGeneratedEvent;
import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.AlertEscalationPolicy;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventCommand;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventUseCase;
import edu.cidemags.integrity_core.sentinel.ports.out.AlertRepositoryPort;
import edu.cidemags.integrity_core.sentinel.ports.out.SuspiciousEventRepositoryPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReceiveSuspiciousEventService implements ReceiveSuspiciousEventUseCase {

    private final SuspiciousEventRepositoryPort eventRepository;
    private final AlertRepositoryPort alertRepository;
    private final AlertEscalationPolicy escalationPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public ReceiveSuspiciousEventService(
        SuspiciousEventRepositoryPort eventRepository,
        AlertRepositoryPort alertRepository,
        AlertEscalationPolicy escalationPolicy,
        ApplicationEventPublisher eventPublisher
    ) {
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.escalationPolicy = escalationPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<Alert> receive(ReceiveSuspiciousEventCommand command) {
        ExamSessionId examSessionId = new ExamSessionId(command.examSessionId());
        StudentId studentId = new StudentId(command.studentId());

        SuspiciousEvent newEvent = new SuspiciousEvent(
            UUID.randomUUID(),
            examSessionId,
            studentId,
            command.type(),
            command.occurredAt()
        );

        List<SuspiciousEvent> previousEvents = eventRepository.findByExamSessionId(examSessionId);
        List<SuspiciousEvent> allEvents = new ArrayList<>(previousEvents);
        allEvents.add(newEvent);

        eventRepository.save(newEvent);

        Optional<Alert> generatedAlert = escalationPolicy.evaluate(allEvents);

        generatedAlert.ifPresent(alert -> {
            alertRepository.save(alert);
            eventPublisher.publishEvent(new AlertGeneratedEvent(alert));
        });

        return generatedAlert;
    }
}


    

