package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// createdByUserId se inyecta desde el token en el controlador, por lo que no
// recibe restriccion: un valor enviado por el cliente se ignora.
public record CreateExamCommand(@NotBlank(message = "El titulo del examen no puede estar vacio")
		@Size(max = 200, message = "El titulo del examen no puede exceder 200 caracteres") String title,
		@Size(max = 2000, message = "La descripcion del examen no puede exceder 2000 caracteres") String description,
		UUID createdByUserId,
		@Valid @Size(max = 100, message = "El examen no puede tener mas de 100 preguntas") List<CreateQuestionCommand> questions) {
	public CreateExamCommand(String title, String description, UUID createdByUserId) {
		this(title, description, createdByUserId, List.of());
	}
}
