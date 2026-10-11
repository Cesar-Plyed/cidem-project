package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// bankId lo fija el controlador desde la ruta.
public record UpdateQuestionBankCommand(UUID bankId,
        @NotBlank(message = "El nombre del banco no puede estar vacio")
        @Size(max = 200, message = "El nombre del banco no puede exceder 200 caracteres") String name,
        @Size(max = 2000, message = "La descripcion del banco no puede exceder 2000 caracteres") String description) {
}
