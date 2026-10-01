package edu.cidemags.integrity_core.sentinel.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuspiciousEventTest {

    private final UUID id = UUID.randomUUID();
    private final ExamSessionId examSessionId = new ExamSessionId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final AttemptId attemptId = new AttemptId(UUID.randomUUID());
    private final EventType type = EventType.WINDOW_FOCUS_LOST;
    private final Instant occurredAt = Instant.now();

    @Test
    void shouldCreateSuspiciousEventWithValidFields() {
        SuspiciousEvent event = new SuspiciousEvent(id, examSessionId, studentId, attemptId, type, occurredAt);

        assertThat(event.id()).isEqualTo(id);
        assertThat(event.examSessionId()).isEqualTo(examSessionId);
        assertThat(event.studentId()).isEqualTo(studentId);
        assertThat(event.attemptId()).isEqualTo(attemptId);
        assertThat(event.type()).isEqualTo(type);
        assertThat(event.occurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThatThrownBy(() -> new SuspiciousEvent(null, examSessionId, studentId, attemptId, type, occurredAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id");
    }

    @Test
    void shouldThrowExceptionWhenExamSessionIdIsNull() {
        assertThatThrownBy(() -> new SuspiciousEvent(id, null, studentId, attemptId, type, occurredAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("examSessionId");
    }

    @Test
    void shouldThrowExceptionWhenStudentIdIsNull() {
        assertThatThrownBy(() -> new SuspiciousEvent(id, examSessionId, null, attemptId, type, occurredAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("studentId");
    }

    @Test
    void shouldThrowExceptionWhenAttemptIdIsNull() {
        assertThatThrownBy(() -> new SuspiciousEvent(id, examSessionId, studentId, null, type, occurredAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("attemptId");
    }

    @Test
    void shouldThrowExceptionWhenTypeIsNull() {
        assertThatThrownBy(() -> new SuspiciousEvent(id, examSessionId, studentId, attemptId, null, occurredAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("type");
    }

    @Test
    void shouldThrowExceptionWhenOccurredAtIsNull() {
        assertThatThrownBy(() -> new SuspiciousEvent(id, examSessionId, studentId, attemptId, type, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("occurredAt");
    }
}