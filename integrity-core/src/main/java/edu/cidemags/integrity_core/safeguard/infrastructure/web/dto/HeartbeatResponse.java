package edu.cidemags.integrity_core.safeguard.infrastructure.web.dto;

import java.time.Instant;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.application.dto.HeartbeatResult;

public record HeartbeatResponse(
        SafeguardSessionStatus status,
        int riskScore,
        Instant serverTime,
        boolean closed) {

    public static HeartbeatResponse from(HeartbeatResult r) {
        return new HeartbeatResponse(r.status(), r.riskScore(), r.serverTime(), r.closed());
    }
}