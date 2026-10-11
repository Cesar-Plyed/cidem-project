package edu.cidemags.integrity_core.shared.events;

import java.time.Instant;
import java.util.UUID;

public record ExamAttemptRevokedEvent(UUID examId, UUID attemptId, UUID studentId, Instant occurredAt) {
}
