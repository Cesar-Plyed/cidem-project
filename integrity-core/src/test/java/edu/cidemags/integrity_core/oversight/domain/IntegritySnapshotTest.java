package edu.cidemags.integrity_core.oversight.domain;

import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegritySnapshotTest {

    private final UUID id = UUID.randomUUID();
    private final ExamSessionId examSessionId = new ExamSessionId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final AttemptId attemptId = new AttemptId(UUID.randomUUID());
    private final int score = 85;
    private final RiskLevel riskLevel = RiskLevel.LOW;
    private final Instant lastUpdatedAt = Instant.now();

    @Test
    void shouldCreateIntegritySnapshotWithValidFields() {
        IntegritySnapshot snapshot = new IntegritySnapshot(id, examSessionId, studentId, attemptId, score, riskLevel, lastUpdatedAt);

        assertThat(snapshot.id()).isEqualTo(id);
        assertThat(snapshot.examSessionId()).isEqualTo(examSessionId);
        assertThat(snapshot.studentId()).isEqualTo(studentId);
        assertThat(snapshot.attemptId()).isEqualTo(attemptId);
        assertThat(snapshot.score()).isEqualTo(score);
        assertThat(snapshot.riskLevel()).isEqualTo(riskLevel);
        assertThat(snapshot.lastUpdatedAt()).isEqualTo(lastUpdatedAt);
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThatThrownBy(() -> new IntegritySnapshot(null, examSessionId, studentId, attemptId, score, riskLevel, lastUpdatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id");
    }

    @Test
    void shouldThrowExceptionWhenExamSessionIdIsNull() {
        assertThatThrownBy(() -> new IntegritySnapshot(id, null, studentId, attemptId, score, riskLevel, lastUpdatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("examSessionId");
    }

    @Test
    void shouldThrowExceptionWhenStudentIdIsNull() {
        assertThatThrownBy(() -> new IntegritySnapshot(id, examSessionId, null, attemptId, score, riskLevel, lastUpdatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("studentId");
    }

    @Test
    void shouldThrowExceptionWhenAttemptIdIsNull() {
        assertThatThrownBy(() -> new IntegritySnapshot(id, examSessionId, studentId, null, score, riskLevel, lastUpdatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("attemptId");
    }

    @Test
    void shouldThrowExceptionWhenScoreIsBelowZero() {
        assertThatThrownBy(() -> new IntegritySnapshot(id, examSessionId, studentId, attemptId, -1, riskLevel, lastUpdatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("score");
    }

    @Test
    void shouldThrowExceptionWhenScoreIsAboveOneHundred() {
        assertThatThrownBy(() -> new IntegritySnapshot(id, examSessionId, studentId, attemptId, 101, riskLevel, lastUpdatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("score");
    }

    @Test
    void shouldThrowExceptionWhenRiskLevelIsNull() {
        assertThatThrownBy(() -> new IntegritySnapshot(id, examSessionId, studentId, attemptId, score, null, lastUpdatedAt))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("riskLevel");
    }

    @Test
    void shouldThrowExceptionWhenLastUpdatedAtIsNull() {
        assertThatThrownBy(() -> new IntegritySnapshot(id, examSessionId, studentId, attemptId, score, riskLevel, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lastUpdatedAt");
    }
}