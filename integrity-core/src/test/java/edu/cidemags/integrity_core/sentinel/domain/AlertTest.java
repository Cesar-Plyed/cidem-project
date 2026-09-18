package edu.cidemags.integrity_core.sentinel.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertTest {

    private final UUID id = UUID.randomUUID();
    private final ExamSessionId examSessionId = new ExamSessionId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final AlertSeverity severity = AlertSeverity.MEDIUM;
    private final String reason = "Múltiples pérdidas de foco detectadas";
    private final List<UUID> sourceEventIds = List.of(UUID.randomUUID());
    private final Instant generatedAt = Instant.now();

    @Test
    void shouldCreateAlertWithValidFields() {
        Alert alert = new Alert(id, examSessionId, studentId, severity, reason, sourceEventIds, generatedAt);

        assertThat(alert.id()).isEqualTo(id);
        assertThat(alert.examSessionId()).isEqualTo(examSessionId);
        assertThat(alert.studentId()).isEqualTo(studentId);
        assertThat(alert.severity()).isEqualTo(severity);
        assertThat(alert.reason()).isEqualTo(reason);
        assertThat(alert.sourceEventIds()).isEqualTo(sourceEventIds);
        assertThat(alert.generatedAt()).isEqualTo(generatedAt);
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThatThrownBy(() -> new Alert(null, examSessionId, studentId, severity, reason, sourceEventIds, generatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id");
    }

    @Test
    void shouldThrowExceptionWhenExamSessionIdIsNull() {
        assertThatThrownBy(() -> new Alert(id, null, studentId, severity, reason, sourceEventIds, generatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("examSessionId");
    }

    @Test
    void shouldThrowExceptionWhenStudentIdIsNull() {
        assertThatThrownBy(() -> new Alert(id, examSessionId, null, severity, reason, sourceEventIds, generatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("studentId");
    }

    @Test
    void shouldThrowExceptionWhenSeverityIsNull() {
        assertThatThrownBy(() -> new Alert(id, examSessionId, studentId, null, reason, sourceEventIds, generatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("severity");
    }

    @Test
    void shouldThrowExceptionWhenReasonIsBlank() {
        assertThatThrownBy(() -> new Alert(id, examSessionId, studentId, severity, "  ", sourceEventIds, generatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reason");
    }

    @Test
    void shouldThrowExceptionWhenSourceEventIdsIsEmpty() {
        assertThatThrownBy(() -> new Alert(id, examSessionId, studentId, severity, reason, List.of(), generatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("evento");
    }

    @Test
    void shouldThrowExceptionWhenGeneratedAtIsNull() {
        assertThatThrownBy(() -> new Alert(id, examSessionId, studentId, severity, reason, sourceEventIds, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("generatedAt");
    }
}