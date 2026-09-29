package edu.cidemags.integrity_core.safeguard.web.dto;

import java.time.Instant;

import edu.cidemags.integrity_core.safeguard.domain.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.HeartbeatResult;

public record HeartbeatResponse(
        SafeguardSessionStatus status,
        int riskScore,
        Instant serverTime,
        boolean closed) {

    public static HeartbeatResponse from(HeartbeatResult r) {
        return new HeartbeatResponse(r.status(), r.riskScore(), r.serverTime(), r.closed());
    }
}