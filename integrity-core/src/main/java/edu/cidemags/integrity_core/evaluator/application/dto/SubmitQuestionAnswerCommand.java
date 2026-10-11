package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// attemptId lo fija el controlador desde la ruta.
public record SubmitQuestionAnswerCommand(UUID attemptId,
		@NotNull(message = "La pregunta respondida es obligatoria") UUID questionId,
		@Size(max = 20, message = "No se pueden seleccionar mas de 20 opciones") List<UUID> selectedOptionIds,
		@Size(max = 20000, message = "La respuesta de texto no puede exceder 20000 caracteres") String openTextAnswer) {
}
