package edu.cidemags.integrity_core.safeguard.infrastructure.web.dto;

import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.application.dto.RecordEventResult;

public record RecordEventResponse(
        UUID id,
        UUID eventId,
        int severity,
        int riskPoints,
        int totalRiskScore,
        SafeguardSessionStatus sessionStatus,
        boolean duplicate) {

    public static RecordEventResponse from(RecordEventResult r) {
        return new RecordEventResponse(r.id(), r.eventId(), r.severity(), r.riskPoints(),
                r.totalRiskScore(), r.sessionStatus(), r.duplicate());
    }
}