package edu.cidemags.integrity_core.evaluator.application.dto;

import edu.cidemags.integrity_core.evaluator.domain.model.QuestionType;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionCommand(
		@NotBlank(message = "El enunciado de la pregunta no puede estar vacio")
		@Size(max = 5000, message = "El enunciado de la pregunta no puede exceder 5000 caracteres") String prompt,
		@NotNull(message = "El tipo de pregunta es obligatorio") QuestionType type,
		@Valid @Size(max = 20, message = "Una pregunta no puede tener mas de 20 opciones") List<QuestionOptionCommand> options,
		@Size(max = 5000, message = "La respuesta esperada no puede exceder 5000 caracteres") String correctAnswerText) {
	public record QuestionOptionCommand(
			@NotBlank(message = "El texto de la opcion no puede estar vacio")
			@Size(max = 1000, message = "El texto de la opcion no puede exceder 1000 caracteres") String text,
			boolean correct) {
	}
}
