package edu.cidemags.integrity_core.safeguard.application.dto;

import java.util.UUID;

public record ConfigView(UUID examId, boolean enabled, boolean requireCamera, boolean requireMicrophone,
                         boolean requireFullscreen, boolean blockCopyPaste, int maxTabSwitches,
                         int riskThreshold, boolean autoTerminateOnThreshold) {}
