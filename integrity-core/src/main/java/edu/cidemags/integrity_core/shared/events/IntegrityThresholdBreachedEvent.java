package edu.cidemags.integrity_core.shared.events;

import java.time.Instant;
import java.util.UUID;

public record IntegrityThresholdBreachedEvent(
        UUID examId,
        UUID attemptId,
        UUID studentId,
        int integrityIndex,
        Instant occurredAt) {
}
