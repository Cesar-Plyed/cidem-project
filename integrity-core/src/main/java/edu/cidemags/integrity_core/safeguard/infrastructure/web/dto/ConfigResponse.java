package edu.cidemags.integrity_core.safeguard.infrastructure.web.dto;

import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.application.dto.ConfigView;

public record ConfigResponse(
        UUID examId,
        boolean enabled,
        boolean requireCamera,
        boolean requireMicrophone,
        boolean requireFullscreen,
        boolean blockCopyPaste,
        int maxTabSwitches,
        int riskThreshold,
        boolean autoTerminateOnThreshold) {

    public static ConfigResponse from(ConfigView c) {
        return new ConfigResponse(c.examId(), c.enabled(), c.requireCamera(), c.requireMicrophone(),
                c.requireFullscreen(), c.blockCopyPaste(), c.maxTabSwitches(),
                c.riskThreshold(), c.autoTerminateOnThreshold());
    }
}
