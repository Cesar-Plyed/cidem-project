package edu.cidemags.integrity_core.evaluator.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// studentId lo fija el controlador desde el token.
public record JoinExamByAccessCodeCommand(
        @NotNull(message = "El identificador del examen es obligatorio") UUID examId,
        @NotBlank(message = "El codigo de acceso es obligatorio")
        @Size(max = 64, message = "El codigo de acceso no puede exceder 64 caracteres") String code,
        UUID studentId) {
}
