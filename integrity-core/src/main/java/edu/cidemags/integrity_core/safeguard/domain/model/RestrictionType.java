package edu.cidemags.integrity_core.safeguard.domain.model;

/**
 * Tipos de restricción configurables por examen (spec 19).
 *
 * <p>Se mantiene separado de {@link SafeguardEventType}, que describe todos los
 * eventos que el monitor puede detectar: aquí solo viven las condiciones que el
 * docente puede limitar con un umbral y una ventana.</p>
 */
public enum RestrictionType {
    TAB_SWITCH,
    FOCUS_LOSS,
    COPY_PASTE,
    DISCONNECTION
}
