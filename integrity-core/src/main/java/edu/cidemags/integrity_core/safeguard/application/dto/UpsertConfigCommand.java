package edu.cidemags.integrity_core.safeguard.application.dto;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;

/**
 * Comando de upsert de configuración (spec 19). {@code restrictions} es opcional:
 * {@code null} o vacío significa "no tocar las restricciones existentes".
 */
public record UpsertConfigCommand(UUID examId, boolean enabled, boolean requireCamera,
                                  boolean requireMicrophone, boolean requireFullscreen,
                                  boolean blockCopyPaste, int maxTabSwitches, int riskThreshold,
                                  boolean autoTerminateOnThreshold, List<Restriction> restrictions,
                                  boolean allowExternalApps, boolean allowExternalTabs) {

    /** Constructor compatible con las firmas previas (sin restricciones ni flags de alcance). */
    public UpsertConfigCommand(UUID examId, boolean enabled, boolean requireCamera,
                               boolean requireMicrophone, boolean requireFullscreen,
                               boolean blockCopyPaste, int maxTabSwitches, int riskThreshold,
                               boolean autoTerminateOnThreshold) {
        this(examId, enabled, requireCamera, requireMicrophone, requireFullscreen, blockCopyPaste,
                maxTabSwitches, riskThreshold, autoTerminateOnThreshold, null, false, false);
    }
}
