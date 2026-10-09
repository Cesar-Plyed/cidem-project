package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// examId lo fija el controlador desde la ruta; solo title/description vienen del cuerpo.
public record UpdateExamCommand(UUID examId,
		@NotBlank(message = "El titulo del examen no puede estar vacio")
		@Size(max = 200, message = "El titulo del examen no puede exceder 200 caracteres") String title,
		@Size(max = 2000, message = "La descripcion del examen no puede exceder 2000 caracteres") String description) {
}
