package edu.cidemags.integrity_core.sentinel.infrastructure.adapter;

import edu.cidemags.integrity_core.sentinel.application.dto.SentinelEventView;

import java.time.Instant;
import java.util.UUID;

/** Respuesta REST de un evento sospechoso. */
public record SentinelEventResponse(
    UUID id,
    String eventId,
    UUID examId,
    UUID examSessionId,
    UUID attemptId,
    UUID studentId,
    String eventType,
    String source,
    String payload,
    Instant occurredAt
) {

    public static SentinelEventResponse from(SentinelEventView view) {
        return new SentinelEventResponse(
            view.id(),
            view.eventId(),
            view.examId(),
            view.examSessionId(),
            view.attemptId(),
            view.studentId(),
            view.eventType(),
            view.source(),
            view.payload(),
            view.occurredAt()
        );
    }
}
