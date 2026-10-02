package edu.cidemags.integrity_core.oversight.domain;

import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
@Component 
public class IntegrityScoringPolicy {

    private static final int INITIAL_SCORE = 100;
    private static final int LOW_PENALTY = 5;
    private static final int MEDIUM_PENALTY = 10;
    private static final int HIGH_PENALTY = 20;

    private static final int LOW_RISK_THRESHOLD = 80;
    private static final int MEDIUM_RISK_THRESHOLD = 50;
    private static final int HIGH_RISK_THRESHOLD = 20;

    public IntegritySnapshot initialize(ExamSessionId examSessionId, StudentId studentId, AttemptId attemptId) {
        return new IntegritySnapshot(
            UUID.randomUUID(), examSessionId, studentId, attemptId,
            INITIAL_SCORE, RiskLevel.LOW, Instant.now()
        );
    }

    public IntegritySnapshot applyLogEntry(IntegritySnapshot current, LogEntry entry) {
        int penalty = switch (entry.severity()) {
            case LOW -> LOW_PENALTY;
            case MEDIUM -> MEDIUM_PENALTY;
            case HIGH -> HIGH_PENALTY;
        };

        int newScore = Math.max(0, current.score() - penalty);

        return new IntegritySnapshot(
            current.id(), current.examSessionId(), current.studentId(), current.attemptId(),
            newScore, classify(newScore), Instant.now()
        );
    }

    private RiskLevel classify(int score) {
        if (score >= LOW_RISK_THRESHOLD) {
            return RiskLevel.LOW;
        }
        if (score >= MEDIUM_RISK_THRESHOLD) {
            return RiskLevel.MEDIUM;
        }
        if (score >= HIGH_RISK_THRESHOLD) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.CRITICAL;
    }
}