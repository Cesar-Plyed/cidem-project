package edu.cidemags.integrity_core.sentinel.ports.in;

import edu.cidemags.integrity_core.sentinel.domain.EventType;

import java.time.Instant;
import java.util.UUID;

public record ReceiveSuspiciousEventCommand(
    UUID examSessionId,
    UUID studentId,
    UUID attemptId,
    EventType type,
    Instant occurredAt
) {
}