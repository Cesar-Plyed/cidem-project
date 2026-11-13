package edu.cidemags.integrity_core.safeguard.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionSeverity;
import edu.cidemags.integrity_core.safeguard.domain.model.RestrictionType;

/**
 * Restricción enviada en el PUT de configuración (spec 19). Los límites replican
 * los CHECK constraints de safeguard_restriction (V16).
 */
public record SaveRestrictionRequest(
        @NotNull(message = "type es obligatorio") RestrictionType type,
        boolean enabled,
        @Min(value = 1, message = "maxOccurrences debe ser > 0") int maxOccurrences,
        @Min(value = 1, message = "windowSeconds debe ser > 0") int windowSeconds,
        @NotNull(message = "severity es obligatoria") RestrictionSeverity severity) {}
