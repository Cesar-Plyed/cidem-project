package edu.cidemags.integrity_core.oversight.domain;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrityScoringPolicyTest {

    private final IntegrityScoringPolicy policy = new IntegrityScoringPolicy();
    private final ExamSessionId examSessionId = new ExamSessionId(UUID.randomUUID());
    private final StudentId studentId = new StudentId(UUID.randomUUID());
    private final AttemptId attemptId = new AttemptId(UUID.randomUUID());

    private LogEntry logEntry(AlertSeverity severity) {
        return new LogEntry(
            UUID.randomUUID(), examSessionId, studentId, attemptId,
            LogSource.SENTINEL, severity, "Evento de prueba", Instant.now(), Instant.now()
        );
    }

    @Test
    void shouldInitializeWithFullScoreAndLowRisk() {
        IntegritySnapshot snapshot = policy.initialize(examSessionId, studentId, attemptId);

        assertThat(snapshot.score()).isEqualTo(100);
        assertThat(snapshot.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(snapshot.examSessionId()).isEqualTo(examSessionId);
        assertThat(snapshot.studentId()).isEqualTo(studentId);
        assertThat(snapshot.attemptId()).isEqualTo(attemptId);
    }

    @Test
    void shouldApplyLowSeverityPenalty() {
        IntegritySnapshot initial = policy.initialize(examSessionId, studentId, attemptId);

        IntegritySnapshot updated = policy.applyLogEntry(initial, logEntry(AlertSeverity.LOW));

        assertThat(updated.score()).isEqualTo(95);
        assertThat(updated.riskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void shouldApplyMediumSeverityPenalty() {
        IntegritySnapshot initial = policy.initialize(examSessionId, studentId, attemptId);

        IntegritySnapshot updated = policy.applyLogEntry(initial, logEntry(AlertSeverity.MEDIUM));

        assertThat(updated.score()).isEqualTo(90);
        assertThat(updated.riskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void shouldApplyHighSeverityPenalty() {
        IntegritySnapshot initial = policy.initialize(examSessionId, studentId, attemptId);

        IntegritySnapshot updated = policy.applyLogEntry(initial, logEntry(AlertSeverity.HIGH));

        assertThat(updated.score()).isEqualTo(80);
        assertThat(updated.riskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void shouldReclassifyToMediumRiskWhenScoreDropsBelowEighty() {
        IntegritySnapshot snapshot = policy.initialize(examSessionId, studentId, attemptId);
        snapshot = policy.applyLogEntry(snapshot, logEntry(AlertSeverity.HIGH));
        snapshot = policy.applyLogEntry(snapshot, logEntry(AlertSeverity.LOW));

        assertThat(snapshot.score()).isEqualTo(75);
        assertThat(snapshot.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void shouldReclassifyToHighRiskWhenScoreDropsBelowFifty() {
        IntegritySnapshot snapshot = policy.initialize(examSessionId, studentId, attemptId);
        for (int i = 0; i < 3; i++) {
            snapshot = policy.applyLogEntry(snapshot, logEntry(AlertSeverity.HIGH));
        }

        assertThat(snapshot.score()).isEqualTo(40);
        assertThat(snapshot.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void shouldReclassifyToCriticalRiskWhenScoreDropsBelowTwenty() {
        IntegritySnapshot snapshot = policy.initialize(examSessionId, studentId, attemptId);
        for (int i = 0; i < 4; i++) {
            snapshot = policy.applyLogEntry(snapshot, logEntry(AlertSeverity.HIGH));
        }

        assertThat(snapshot.score()).isEqualTo(20);
        assertThat(snapshot.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void shouldNeverDropScoreBelowZero() {
        IntegritySnapshot snapshot = policy.initialize(examSessionId, studentId, attemptId);
        for (int i = 0; i < 10; i++) {
            snapshot = policy.applyLogEntry(snapshot, logEntry(AlertSeverity.HIGH));
        }

        assertThat(snapshot.score()).isEqualTo(0);
        assertThat(snapshot.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void shouldPreserveIdentityFieldsAfterApplyingLogEntry() {
        IntegritySnapshot initial = policy.initialize(examSessionId, studentId, attemptId);

        IntegritySnapshot updated = policy.applyLogEntry(initial, logEntry(AlertSeverity.LOW));

        assertThat(updated.id()).isEqualTo(initial.id());
        assertThat(updated.examSessionId()).isEqualTo(initial.examSessionId());
        assertThat(updated.studentId()).isEqualTo(initial.studentId());
        assertThat(updated.attemptId()).isEqualTo(initial.attemptId());
    }
}