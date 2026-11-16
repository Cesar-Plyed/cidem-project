package edu.cidemags.integrity_core.evaluator.domain.exception;

/**
 * El examen exige la app de escritorio (browserAllowed=false) y la petición
 * no proviene del cliente Tauri. El handler la mapea a 403 con error
 * DESKTOP_APP_REQUIRED para que el frontend muestre la pantalla de bloqueo.
 */
public class DesktopAppRequiredException extends DomainException {

    public DesktopAppRequiredException(java.util.UUID examId) {
        super("Este examen solo puede realizarse desde la aplicacion de escritorio CIDEM");
    }
}
