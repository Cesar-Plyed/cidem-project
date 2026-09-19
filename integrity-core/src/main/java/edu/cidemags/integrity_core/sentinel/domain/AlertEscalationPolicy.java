package edu.cidemags.integrity_core.sentinel.domain;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AlertEscalationPolicy {

    private static final int FOCUS_LOST_THRESHOLD = 3;
    private static final Duration FOCUS_LOST_WINDOW = Duration.ofMinutes(2);

    public Optional<Alert> evaluate(List<SuspiciousEvent> events) {
        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }

        Optional<Alert> tabSwitchAlert = evaluateImmediateEscalation(events);
        if (tabSwitchAlert.isPresent()) {
            return tabSwitchAlert;
        }

        return evaluateFocusLostPattern(events);
    }

    private Optional<Alert> evaluateImmediateEscalation(List<SuspiciousEvent> events) {
        return events.stream()
            .filter(e -> e.type() == EventType.TAB_OR_APP_SWITCH)
            .findFirst()
            .map(e -> new Alert(
                UUID.randomUUID(),
                e.examSessionId(),
                e.studentId(),
                AlertSeverity.HIGH,
                "Cambio de aplicación/pestaña detectado",
                List.of(e.id()),
                Instant.now()
            ));
    }

    private Optional<Alert> evaluateFocusLostPattern(List<SuspiciousEvent> events) {
    List<SuspiciousEvent> focusLostEvents = events.stream()
        .filter(e -> e.type() == EventType.WINDOW_FOCUS_LOST)
        .sorted(Comparator.comparing(SuspiciousEvent::occurredAt))
        .toList();

    if (focusLostEvents.size() < FOCUS_LOST_THRESHOLD) {
        return Optional.empty();
    }

    int fromIndex = focusLostEvents.size() - FOCUS_LOST_THRESHOLD;
    List<SuspiciousEvent> windowEvents = focusLostEvents.subList(fromIndex, focusLostEvents.size());

    Instant windowStart = windowEvents.get(0).occurredAt();
    Instant windowEnd = windowEvents.get(windowEvents.size() - 1).occurredAt();

    if (Duration.between(windowStart, windowEnd).compareTo(FOCUS_LOST_WINDOW) > 0) {
        return Optional.empty();
    }

    SuspiciousEvent anyEvent = windowEvents.get(0);
    List<UUID> sourceIds = windowEvents.stream().map(SuspiciousEvent::id).toList();

    return Optional.of(new Alert(
        UUID.randomUUID(),
        anyEvent.examSessionId(),
        anyEvent.studentId(),
        AlertSeverity.MEDIUM,
        "Múltiples pérdidas de foco detectadas en un periodo corto",
        sourceIds,
        Instant.now()
    ));
}

 
}