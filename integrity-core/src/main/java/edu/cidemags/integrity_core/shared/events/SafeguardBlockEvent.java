package edu.cidemags.integrity_core.shared.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato estándar de eventos de bloqueo de SafeGuard (ToDo.md, Sprint 1).
 * Publica este evento quien lo bloquee; lo consumen EVALUATOR y OVERSIGHT.
 * Campos estándar: eventId, examId, attemptId, examSessionId, studentId,
 * occurredAt, source, payload.
 * <p>
 * Ubicado en {@code shared.events} (paquete compartido de contratos entre módulos,
 * decisión tomada sobre {@code shared_kernel}, que queda para excepciones/utilidades).
 */
public record SafeguardBlockEvent(
        UUID eventId,
        UUID examId,
        UUID attemptId,
        UUID examSessionId,
        UUID studentId,
        Instant occurredAt,
        String source,
        Map<String, Object> payload) {

    /** Valor estándar de {@code source} cuando el evento lo emite SafeGuard. */
    public static final String SOURCE_SAFEGUARD = "safeguard";
}
