package edu.cidemags.integrity_core.oversight.adapters.in.web;

import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;
import edu.cidemags.integrity_core.oversight.domain.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record IntegritySnapshotResponse(
    UUID studentId,
    UUID attemptId,
    int score,
    RiskLevel riskLevel,
    Instant lastUpdatedAt
) {
    public static IntegritySnapshotResponse from(IntegritySnapshot snapshot) {
        return new IntegritySnapshotResponse(
            snapshot.studentId().value(),
            snapshot.attemptId().value(),
            snapshot.score(),
            snapshot.riskLevel(),
            snapshot.lastUpdatedAt()
        );
    }
}