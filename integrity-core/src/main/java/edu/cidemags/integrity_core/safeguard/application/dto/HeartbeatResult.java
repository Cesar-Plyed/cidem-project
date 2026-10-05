package edu.cidemags.integrity_core.safeguard.application.dto;

import java.time.Instant;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;

public record HeartbeatResult(SafeguardSessionStatus status, int riskScore,
                              Instant serverTime, boolean closed) {}
