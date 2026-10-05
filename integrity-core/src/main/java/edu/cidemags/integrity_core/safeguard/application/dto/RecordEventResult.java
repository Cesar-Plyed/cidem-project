package edu.cidemags.integrity_core.safeguard.application.dto;

import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;

public record RecordEventResult(UUID id, UUID eventId, int severity, int riskPoints,
                                int totalRiskScore, SafeguardSessionStatus sessionStatus,
                                boolean duplicate) {}
