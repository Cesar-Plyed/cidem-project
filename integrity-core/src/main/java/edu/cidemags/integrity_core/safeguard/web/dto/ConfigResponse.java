package edu.cidemags.integrity_core.safeguard.web.dto;

import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.service.SafeguardService.ConfigView;

public record ConfigResponse(
        UUID examId,
        boolean enabled,
        boolean requireCamera,
        boolean requireMicrophone,
        boolean requireFullscreen,
        boolean blockCopyPaste,
        int maxTabSwitches) {

    public static ConfigResponse from(ConfigView c) {
        return new ConfigResponse(c.examId(), c.enabled(), c.requireCamera(), c.requireMicrophone(),
                c.requireFullscreen(), c.blockCopyPaste(), c.maxTabSwitches());
    }
}