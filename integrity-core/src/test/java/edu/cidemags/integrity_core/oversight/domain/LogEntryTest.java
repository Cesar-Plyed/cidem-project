package edu.cidemags.integrity_core.oversight.domain;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogEntryTest {

    private final UUID id = UUID.randomUUID();
    private final ExamSessionId examSessionId = new ExamSessionId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final AttemptId attemptId = new AttemptId(UUID.randomUUID());
    private final LogSource source = LogSource.SENTINEL;
    private final AlertSeverity severity = AlertSeverity.MEDIUM;
    private final String description = "Múltiples pérdidas de foco detectadas";
    private final Instant occurredAt = Instant.now();
    private final Instant recordedAt = Instant.now();

    @Test
    void shouldCreateLogEntryWithValidFields() {
        LogEntry entry = new LogEntry(id, examSessionId, studentId, attemptId, source, severity, description, occurredAt, recordedAt);

        assertThat(entry.id()).isEqualTo(id);
        assertThat(entry.examSessionId()).isEqualTo(examSessionId);
        assertThat(entry.studentId()).isEqualTo(studentId);
        assertThat(entry.attemptId()).isEqualTo(attemptId);
        assertThat(entry.source()).isEqualTo(source);
        assertThat(entry.severity()).isEqualTo(severity);
        assertThat(entry.description()).isEqualTo(description);
        assertThat(entry.occurredAt()).isEqualTo(occurredAt);
        assertThat(entry.recordedAt()).isEqualTo(recordedAt);
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThatThrownBy(() -> new LogEntry(null, examSessionId, studentId, attemptId, source, severity, description, occurredAt, recordedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id");
    }

    @Test
    void shouldThrowExceptionWhenExamSessionIdIsNull() {
        assertThatThrownBy(() -> new LogEntry(id, null, studentId, attemptId, source, severity, description, occurredAt, recordedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("examSessionId");
    }

    @Test
    void shouldThrowExceptionWhenStudentIdIsNull() {
        assertThatThrownBy(() -> new LogEntry(id, examSessionId, null, attemptId, source, severity, description, occurredAt, recordedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("studentId");
    }

    @Test
    void shouldThrowExceptionWhenAttemptIdIsNull() {
        assertThatThrownBy(() -> new LogEntry(id, examSessionId, studentId, null, source, severity, description, occurredAt, recordedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("attemptId");
    }

    @Test
    void shouldThrowExceptionWhenSourceIsNull() {
        assertThatThrownBy(() -> new LogEntry(id, examSessionId, studentId, attemptId, null, severity, description, occurredAt, recordedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("source");
    }

    @Test
    void shouldThrowExceptionWhenSeverityIsNull() {
        assertThatThrownBy(() -> new LogEntry(id, examSessionId, studentId, attemptId, source, null, description, occurredAt, recordedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("severity");
    }

    @Test
    void shouldThrowExceptionWhenDescriptionIsBlank() {
        assertThatThrownBy(() -> new LogEntry(id, examSessionId, studentId, attemptId, source, severity, "  ", occurredAt, recordedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("description");
    }

    @Test
    void shouldThrowExceptionWhenOccurredAtIsNull() {
        assertThatThrownBy(() -> new LogEntry(id, examSessionId, studentId, attemptId, source, severity, description, null, recordedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("occurredAt");
    }

    @Test
    void shouldThrowExceptionWhenRecordedAtIsNull() {
        assertThatThrownBy(() -> new LogEntry(id, examSessionId, studentId, attemptId, source, severity, description, occurredAt, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("recordedAt");
    }
}