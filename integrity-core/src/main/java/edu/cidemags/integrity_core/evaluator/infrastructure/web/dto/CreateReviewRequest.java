package edu.cidemags.integrity_core.evaluator.infrastructure.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Cuerpo del POST /reviews/open-ended. reviewerId y administrator vienen de la
// sesion, por lo que no forman parte del cuerpo (evita el fallo de
// deserializacion de `boolean` primitivo cuando el cliente no los envia).
public record CreateReviewRequest(
        @NotNull(message = "La respuesta a revisar es obligatoria") UUID answerId,
        @Min(value = 0, message = "La calificacion no puede ser negativa") int score,
        @Size(max = 2000, message = "El comentario no puede exceder 2000 caracteres") String feedback) {
}
