package edu.cidemags.integrity_core.safeguard.application.dto;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;

public record SessionView(UUID sessionId, UUID examId, UUID attemptId,
                          SafeguardSessionStatus status, int riskScore,
                          Instant startedAt, Instant endedAt, Instant lastHeartbeatAt,
                          boolean requireCamera, boolean requireMicrophone,
                          boolean requireFullscreen, boolean blockCopyPaste) {}
