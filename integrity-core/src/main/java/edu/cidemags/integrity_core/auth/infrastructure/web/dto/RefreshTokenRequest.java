package edu.cidemags.integrity_core.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {
}