package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// reviewerId y administrator los fija el controlador desde la sesion.
public record ReviewOpenEndedAnswerCommand(
        @NotNull(message = "La respuesta a revisar es obligatoria") UUID answerId,
        UUID reviewerId,
        @Min(value = 0, message = "La calificacion no puede ser negativa") int score,
        @Size(max = 2000, message = "El comentario no puede exceder 2000 caracteres") String feedback,
        boolean administrator) {
    public ReviewOpenEndedAnswerCommand(UUID answerId, UUID reviewerId, int score, String feedback) {
        this(answerId, reviewerId, score, feedback, false);
    }
}
