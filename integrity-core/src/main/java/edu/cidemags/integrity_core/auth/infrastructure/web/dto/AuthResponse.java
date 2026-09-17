package edu.cidemags.integrity_core.auth.infrastructure.web.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}