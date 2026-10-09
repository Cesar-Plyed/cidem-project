package edu.cidemags.integrity_core.evaluator.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewOpenEndedAnswerRequest(
        @Min(value = 0, message = "La calificacion no puede ser negativa") int score,
        @Size(max = 2000, message = "El comentario no puede exceder 2000 caracteres") String feedback) {
    public ReviewOpenEndedAnswerRequest(int score) {
        this(score, null);
    }
}
