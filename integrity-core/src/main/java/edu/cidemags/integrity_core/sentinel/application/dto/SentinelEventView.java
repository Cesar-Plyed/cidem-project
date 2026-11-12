package edu.cidemags.integrity_core.sentinel.application.dto;

import java.time.Instant;
import java.util.UUID;

/** Vista de un evento sospechoso lista para la API. */
public record SentinelEventView(
    UUID id,
    String eventId,
    UUID examId,
    UUID examSessionId,
    UUID studentId,
    UUID attemptId,
    String eventType,
    String source,
    String payload,
    Instant occurredAt
) {
}
