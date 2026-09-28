package edu.cidemags.integrity_core.sentinel.application;

import edu.cidemags.integrity_core.sentinel.application.event.AlertGeneratedEvent;
import edu.cidemags.integrity_core.sentinel.domain.*;
import edu.cidemags.integrity_core.sentinel.ports.in.ReceiveSuspiciousEventCommand;
import edu.cidemags.integrity_core.sentinel.ports.out.AlertRepositoryPort;
import edu.cidemags.integrity_core.sentinel.ports.out.EventWindowPort;
import edu.cidemags.integrity_core.sentinel.ports.out.SuspiciousEventRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReceiveSuspiciousEventServiceTest {

    private SuspiciousEventRepositoryPort eventRepository;
    private EventWindowPort eventWindowPort;
    private AlertRepositoryPort alertRepository;
    private AlertEscalationPolicy escalationPolicy;
    private ApplicationEventPublisher eventPublisher;
    private ReceiveSuspiciousEventService service;

    private final UUID examSessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        eventRepository = mock(SuspiciousEventRepositoryPort.class);
        eventWindowPort = mock(EventWindowPort.class);
        alertRepository = mock(AlertRepositoryPort.class);
        escalationPolicy = mock(AlertEscalationPolicy.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new ReceiveSuspiciousEventService(
            eventRepository, eventWindowPort, alertRepository, escalationPolicy, eventPublisher
        );
    }

    private ReceiveSuspiciousEventCommand command() {
        return new ReceiveSuspiciousEventCommand(
            examSessionId, studentId, attemptId, EventType.WINDOW_FOCUS_LOST, Instant.now()
        );
    }

    @Test
    void shouldSaveEventAndReturnEmptyWhenPolicyDoesNotEscalate() {
        when(eventWindowPort.findRecentEvents(any(), any(), any(), any())).thenReturn(List.of());
        when(escalationPolicy.evaluate(any())).thenReturn(Optional.empty());

        Optional<Alert> result = service.receive(command());

        assertThat(result).isEmpty();
        verify(eventRepository).save(any(SuspiciousEvent.class));
        verify(eventWindowPort).record(any(SuspiciousEvent.class));
        verify(alertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldSaveAlertAndPublishEventWhenPolicyEscalates() {
        Alert alert = new Alert(
            UUID.randomUUID(), new ExamSessionId(examSessionId), new StudentId(studentId), new AttemptId(attemptId),
            AlertSeverity.HIGH, "Cambio de aplicación detectado",
            List.of(UUID.randomUUID()), Instant.now()
        );

        when(eventWindowPort.findRecentEvents(any(), any(), any(), any())).thenReturn(List.of());
        when(escalationPolicy.evaluate(any())).thenReturn(Optional.of(alert));

        Optional<Alert> result = service.receive(command());

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(alert);
        verify(alertRepository).save(alert);

        ArgumentCaptor<AlertGeneratedEvent> captor = ArgumentCaptor.forClass(AlertGeneratedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().alert()).isEqualTo(alert);
    }

    @Test
    void shouldEvaluatePolicyUsingEventsReturnedByEventWindow() {
        SuspiciousEvent previousEvent = new SuspiciousEvent(
            UUID.randomUUID(), new ExamSessionId(examSessionId), new StudentId(studentId), new AttemptId(attemptId),
            EventType.WINDOW_FOCUS_LOST, Instant.now().minusSeconds(30)
        );
        when(eventWindowPort.findRecentEvents(any(), any(), any(), any()))
            .thenReturn(List.of(previousEvent));
        when(escalationPolicy.evaluate(any())).thenReturn(Optional.empty());

        service.receive(command());

        ArgumentCaptor<List<SuspiciousEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(escalationPolicy).evaluate(captor.capture());

        assertThat(captor.getValue()).containsExactly(previousEvent);
    }
}