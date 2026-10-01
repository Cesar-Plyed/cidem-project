package edu.cidemags.integrity_core.safeguard.infrastructure.web.dto;

import jakarta.validation.constraints.Min;

/**
 * Reemplazo completo de la configuración de SafeGuard de un examen (PUT).
 * Los límites replican los CHECK constraints de safeguard_configuration (V7).
 */
public record SaveConfigurationRequest(
        boolean enabled,
        boolean requireCamera,
        boolean requireMicrophone,
        boolean requireFullscreen,
        boolean blockCopyPaste,
        @Min(value = 0, message = "maxTabSwitches debe ser >= 0") int maxTabSwitches,
        @Min(value = 1, message = "riskThreshold debe ser > 0") int riskThreshold,
        boolean autoTerminateOnThreshold) {}
