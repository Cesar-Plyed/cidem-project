package edu.cidemags.integrity_core.safeguard.application.dto;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;

/**
 * Vista de la configuración de SafeGuard de un examen (spec 19). Además de los
 * campos escalares heredados, expone las restricciones por tipo y los dos flags
 * de alcance (apps/pestañas externas).
 */
public record ConfigView(UUID examId, boolean enabled, boolean requireCamera, boolean requireMicrophone,
                         boolean requireFullscreen, boolean blockCopyPaste, int maxTabSwitches,
                         int riskThreshold, boolean autoTerminateOnThreshold, List<Restriction> restrictions,
                         boolean allowExternalApps, boolean allowExternalTabs) {

    /**
     * Constructor compatible con las firmas previas (sin restricciones): deja la
     * lista vacía y los flags de alcance a false.
     */
    public ConfigView(UUID examId, boolean enabled, boolean requireCamera, boolean requireMicrophone,
                      boolean requireFullscreen, boolean blockCopyPaste, int maxTabSwitches,
                      int riskThreshold, boolean autoTerminateOnThreshold) {
        this(examId, enabled, requireCamera, requireMicrophone, requireFullscreen, blockCopyPaste,
                maxTabSwitches, riskThreshold, autoTerminateOnThreshold, List.of(), false, false);
    }
}
