package edu.cidemags.integrity_core.safeguard.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * Reemplazo completo de la configuración de SafeGuard de un examen (PUT).
 * Los límites replican los CHECK constraints de safeguard_configuration (V7).
 * {@code restrictions} es opcional (spec 19): si viene, sustituye por completo las
 * restricciones del examen; si es null/vacío, se conservan las existentes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SaveConfigurationRequest(
        boolean enabled,
        boolean requireCamera,
        boolean requireMicrophone,
        boolean requireFullscreen,
        boolean blockCopyPaste,
        @Min(value = 0, message = "maxTabSwitches debe ser >= 0") int maxTabSwitches,
        @Min(value = 1, message = "riskThreshold debe ser > 0") int riskThreshold,
        boolean autoTerminateOnThreshold,
        @Valid List<SaveRestrictionRequest> restrictions,
        boolean allowExternalApps,
        boolean allowExternalTabs) {

    public SaveConfigurationRequest {
        restrictions = restrictions != null ? restrictions : List.of();
    }
}
