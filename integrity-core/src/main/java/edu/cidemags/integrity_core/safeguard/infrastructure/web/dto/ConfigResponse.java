package edu.cidemags.integrity_core.safeguard.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.application.dto.ConfigView;
import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;

public record ConfigResponse(
        UUID examId,
        boolean enabled,
        boolean requireCamera,
        boolean requireMicrophone,
        boolean requireFullscreen,
        boolean blockCopyPaste,
        int maxTabSwitches,
        int riskThreshold,
        boolean autoTerminateOnThreshold,
        List<Restriction> restrictions,
        boolean allowExternalApps,
        boolean allowExternalTabs) {

    public static ConfigResponse from(ConfigView c) {
        return new ConfigResponse(c.examId(), c.enabled(), c.requireCamera(), c.requireMicrophone(),
                c.requireFullscreen(), c.blockCopyPaste(), c.maxTabSwitches(),
                c.riskThreshold(), c.autoTerminateOnThreshold(), c.restrictions(),
                c.allowExternalApps(), c.allowExternalTabs());
    }
}
