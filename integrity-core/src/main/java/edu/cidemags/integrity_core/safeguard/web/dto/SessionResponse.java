package edu.cidemags.integrity_core.safeguard.web.dto;

import java.time.Instant;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.service.SafeguardService.SessionView;

public record SessionResponse(
        UUID sessionId,
        UUID examId,
        UUID attemptId,
        SafeguardSessionStatus status,
        int riskScore,
        Instant startedAt,
        Instant endedAt,
        Instant lastHeartbeatAt,
        boolean requireCamera,
        boolean requireMicrophone,
        boolean requireFullscreen,
        boolean blockCopyPaste) {

    public static SessionResponse from(SessionView v) {
        return new SessionResponse(v.sessionId(), v.examId(), v.attemptId(), v.status(), v.riskScore(),
                v.startedAt(), v.endedAt(), v.lastHeartbeatAt(), v.requireCamera(), v.requireMicrophone(),
                v.requireFullscreen(), v.blockCopyPaste());
    }
}
