package edu.cidemags.integrity_core.safeguard.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;

public record RecordEventCommand(UUID sessionId, UUID studentId, UUID eventId,
                                 SafeguardEventType eventType, Instant occurredAt, String metadata) {

    private static final int MAX_METADATA_LENGTH = 4000;

    public RecordEventCommand {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        if (metadata != null && metadata.length() > MAX_METADATA_LENGTH) {
            throw new IllegalArgumentException("metadata too large");
        }
    }
}
