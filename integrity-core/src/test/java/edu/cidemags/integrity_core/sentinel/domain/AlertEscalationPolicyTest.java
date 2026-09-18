package edu.cidemags.integrity_core.sentinel.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlertEscalationPolicyTest {

    private final AlertEscalationPolicy policy = new AlertEscalationPolicy();
    private final ExamSessionId examSessionId = new ExamSessionId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());

    private SuspiciousEvent event(EventType type, Instant occurredAt) {
        return new SuspiciousEvent(UUID.randomUUID(), examSessionId, studentId, type, occurredAt);
    }

    @Test
    void shouldReturnEmptyWhenNoEvents() {
        Optional<Alert> result = policy.evaluate(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void shouldEscalateImmediatelyOnTabOrAppSwitch() {
        SuspiciousEvent switchEvent = event(EventType.TAB_OR_APP_SWITCH, Instant.now());

        Optional<Alert> result = policy.evaluate(List.of(switchEvent));

        assertThat(result).isPresent();
        assertThat(result.get().severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(result.get().sourceEventIds()).containsExactly(switchEvent.id());
    }

    @Test
    void shouldNotEscalateWhenFewerThanThreeFocusLostEvents() {
        Instant now = Instant.now();
        List<SuspiciousEvent> events = List.of(
            event(EventType.WINDOW_FOCUS_LOST, now),
            event(EventType.WINDOW_FOCUS_LOST, now.plus(10, ChronoUnit.SECONDS))
        );

        Optional<Alert> result = policy.evaluate(events);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldEscalateWhenThreeFocusLostEventsWithinTwoMinutes() {
        Instant now = Instant.now();
        List<SuspiciousEvent> events = List.of(
            event(EventType.WINDOW_FOCUS_LOST, now),
            event(EventType.WINDOW_FOCUS_LOST, now.plus(30, ChronoUnit.SECONDS)),
            event(EventType.WINDOW_FOCUS_LOST, now.plus(90, ChronoUnit.SECONDS))
        );

        Optional<Alert> result = policy.evaluate(events);

        assertThat(result).isPresent();
        assertThat(result.get().severity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(result.get().sourceEventIds()).hasSize(3);
    }

    @Test
    void shouldNotEscalateWhenThreeFocusLostEventsSpanMoreThanTwoMinutes() {
        Instant now = Instant.now();
        List<SuspiciousEvent> events = List.of(
            event(EventType.WINDOW_FOCUS_LOST, now),
            event(EventType.WINDOW_FOCUS_LOST, now.plus(90, ChronoUnit.SECONDS)),
            event(EventType.WINDOW_FOCUS_LOST, now.plus(150, ChronoUnit.SECONDS))
        );

        Optional<Alert> result = policy.evaluate(events);

        assertThat(result).isEmpty();
    }


    @Test
    void shouldNotEscalateWhenFourthEventBreaksTheWindow() {
        Instant now = Instant.now();
        List<SuspiciousEvent> events = List.of(
            event(EventType.WINDOW_FOCUS_LOST, now),
            event(EventType.WINDOW_FOCUS_LOST, now.plus(40, ChronoUnit.SECONDS)),
            event(EventType.WINDOW_FOCUS_LOST, now.plus(80, ChronoUnit.SECONDS)),
            event(EventType.WINDOW_FOCUS_LOST, now.plus(200, ChronoUnit.SECONDS))
    );

    Optional<Alert> result = policy.evaluate(events);

    assertThat(result).isEmpty();
}



    @Test
void shouldEscalateUsingOnlyLastThreeEventsWhenMoreThanThreeOccur() {
    Instant now = Instant.now();
    List<SuspiciousEvent> events = List.of(
        event(EventType.WINDOW_FOCUS_LOST, now),                              // fuera de la ventana, se ignora
        event(EventType.WINDOW_FOCUS_LOST, now.plus(300, ChronoUnit.SECONDS)),
        event(EventType.WINDOW_FOCUS_LOST, now.plus(330, ChronoUnit.SECONDS)),
        event(EventType.WINDOW_FOCUS_LOST, now.plus(360, ChronoUnit.SECONDS))
    );

    Optional<Alert> result = policy.evaluate(events);

    assertThat(result).isPresent();
    assertThat(result.get().sourceEventIds()).hasSize(3);
}



}