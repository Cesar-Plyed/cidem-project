package edu.cidemags.integrity_core.safeguard.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;

public record RecordEventRequest(
        @NotNull UUID sessionId,
        @NotNull UUID eventId,
        @NotNull SafeguardEventType eventType,
        Instant occurredAt,
        @Size(max = 4000) String metadata) {}
