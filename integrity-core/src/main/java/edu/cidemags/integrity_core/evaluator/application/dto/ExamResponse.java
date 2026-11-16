package edu.cidemags.integrity_core.evaluator.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resumen de examen para listados: sin preguntas (el editor usa GET /{examId})
 * y con la autoria resuelta para mostrarla en el frontend.
 */
public record ExamResponse(
        UUID id,
        String title,
        String description,
        String status,
        int durationMinutes,
        int questionCount,
        OffsetDateTime createdAt,
        UUID createdByUserId,
        String createdByName,
        boolean browserAllowed) {
}
