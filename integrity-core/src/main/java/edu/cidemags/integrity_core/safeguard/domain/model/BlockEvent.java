package edu.cidemags.integrity_core.safeguard.domain.model;

/**
 * Resultado de {@code RestrictionValidator} cuando un evento supera el umbral
 * configurado: describe la restricción, el motivo y el conteo alcanzado.
 */
public record BlockEvent(
    RestrictionType type,
    String reason,
    int occurrences,
    int windowSeconds
) {
}
