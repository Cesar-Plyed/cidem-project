package edu.cidemags.integrity_core.shared.events;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contrato estándar de "examen creado" (publica EVALUATOR, consume SAFEGUARD
 * para siembra automática de la configuración por defecto).
 * <p>
 * Ubicado en {@code shared_events} junto al resto de contratos entre módulos.
 */
public record ExamCreatedEvent(
        UUID examId,
        String title,
        UUID createdByUserId,
        OffsetDateTime createdAt) {
}
