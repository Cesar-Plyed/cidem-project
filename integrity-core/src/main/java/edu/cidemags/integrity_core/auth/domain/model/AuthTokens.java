package edu.cidemags.integrity_core.auth.domain.model;

public record AuthTokens(

        String accessToken,
        String refreshToken

) {
}
