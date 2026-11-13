package edu.cidemags.integrity_core.safeguard.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionType;

/**
 * Evento de restricción detectado que el {@code GuardService} registra y evalúa
 * (spec 20). A diferencia de {@link RecordEventCommand}, el tipo es un
 * {@link RestrictionType} (condición configurable), no un {@code SafeguardEventType}.
 */
public record GuardEventCommand(
        UUID sessionId,
        UUID examId,
        UUID attemptId,
        UUID studentId,
        UUID eventId,
        RestrictionType type,
        Instant occurredAt,
        String metadata) {

    public GuardEventCommand {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(examId, "examId");
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(type, "type");
    }
}
