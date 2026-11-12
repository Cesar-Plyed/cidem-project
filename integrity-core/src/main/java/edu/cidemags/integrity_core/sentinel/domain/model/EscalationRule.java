package edu.cidemags.integrity_core.sentinel.domain.model;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;

import java.time.Duration;

/**
 * Regla de escalamiento de Sentinel: si se acumulan {@code threshold} eventos
 * del mismo tipo dentro de {@code window}, se genera una alerta con la
 * severidad indicada.
 */
public record EscalationRule(
    String ruleName,
    String eventType,
    int threshold,
    Duration window,
    AlertSeverity severity
) {

    public EscalationRule {
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("ruleName es obligatorio");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType es obligatorio");
        }
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold debe ser mayor que cero");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window debe ser positiva");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity es obligatorio");
        }
    }
}
