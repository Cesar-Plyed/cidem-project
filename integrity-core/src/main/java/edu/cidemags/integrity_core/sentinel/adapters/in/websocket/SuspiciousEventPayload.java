package edu.cidemags.integrity_core.sentinel.adapters.in.websocket;

import edu.cidemags.integrity_core.sentinel.domain.EventType;

import java.time.Instant;
import java.util.UUID;

public record SuspiciousEventPayload(
    UUID examSessionId,
    UUID studentId,
    UUID attemptId,
    EventType type,
    Instant occurredAt
) {
}