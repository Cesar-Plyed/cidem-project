package edu.cidemags.integrity_core.safeguard.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Restricción de SafeGuard configurable en un examen (spec 19).
 *
 * <p>{@code maxOccurrences} eventos del mismo {@link RestrictionType} dentro de
 * {@code windowSeconds} son aceptables; el siguiente se considera bloqueo. El
 * examen no forma parte del modelo: la persistencia lo asocia al guardar cada
 * restricción bajo un {@code examId}.</p>
 */
public record Restriction(
    UUID id,
    RestrictionType type,
    boolean enabled,
    int maxOccurrences,
    int windowSeconds,
    RestrictionSeverity severity
) {

    public Restriction {
        if (type == null) {
            throw new IllegalArgumentException("type es obligatorio");
        }
        if (maxOccurrences <= 0) {
            throw new IllegalArgumentException("maxOccurrences debe ser mayor que cero");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds debe ser mayor que cero");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity es obligatoria");
        }
    }

    /** Restricción nueva con id aleatorio y los valores indicados. */
    public static Restriction of(RestrictionType type, boolean enabled, int maxOccurrences,
                                 int windowSeconds, RestrictionSeverity severity) {
        return new Restriction(UUID.randomUUID(), type, enabled, maxOccurrences, windowSeconds, severity);
    }

    /**
     * Configuración por defecto de un examen sin restricciones persistidas: las
     * cuatro condiciones habilitadas con umbrales conservadores.
     */
    public static List<Restriction> defaults() {
        return List.of(
            of(RestrictionType.TAB_SWITCH, true, 3, 300, RestrictionSeverity.LOW),
            of(RestrictionType.FOCUS_LOSS, true, 3, 60, RestrictionSeverity.MEDIUM),
            of(RestrictionType.COPY_PASTE, true, 4, 120, RestrictionSeverity.MEDIUM),
            of(RestrictionType.DISCONNECTION, true, 2, 300, RestrictionSeverity.HIGH)
        );
    }
}
