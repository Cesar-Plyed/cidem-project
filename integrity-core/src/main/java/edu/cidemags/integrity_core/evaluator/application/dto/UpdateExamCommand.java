package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// examId lo fija el controlador desde la ruta; solo title/description vienen del cuerpo.
// questions (opcional) reemplaza el listado completo de preguntas de un examen en
// borrador: si no viene, las preguntas no se tocan.
public record UpdateExamCommand(UUID examId,
		@NotBlank(message = "El titulo del examen no puede estar vacio")
		@Size(max = 200, message = "El titulo del examen no puede exceder 200 caracteres") String title,
		@Size(max = 2000, message = "La descripcion del examen no puede exceder 2000 caracteres") String description,
		@Valid @Size(max = 100, message = "El examen no puede tener mas de 100 preguntas") List<CreateQuestionCommand> questions,
		@NotNull @Min(value = 1, message = "La duracion debe ser al menos un minuto") Integer durationMinutes,
		Boolean browserAllowed) {
	public UpdateExamCommand(UUID examId, String title, String description) {
		this(examId, title, description, null, 30, null);
	}

	public UpdateExamCommand(UUID examId, String title, String description, List<CreateQuestionCommand> questions) {
		this(examId, title, description, questions, 30, null);
	}

	public UpdateExamCommand(UUID examId, String title, String description, List<CreateQuestionCommand> questions,
			Integer durationMinutes) {
		this(examId, title, description, questions, durationMinutes, null);
	}

	/** Normaliza el flag: null significa "sin cambio" (conserva el valor actual). */
	public Boolean browserAllowedOrNull() {
		return browserAllowed;
	}
}
