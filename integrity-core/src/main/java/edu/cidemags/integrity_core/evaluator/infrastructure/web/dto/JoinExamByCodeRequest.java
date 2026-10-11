package edu.cidemags.integrity_core.evaluator.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinExamByCodeRequest(
        @NotBlank(message = "El codigo de acceso es obligatorio")
        @Size(max = 64, message = "El codigo de acceso no puede exceder 64 caracteres") String code) {
}
