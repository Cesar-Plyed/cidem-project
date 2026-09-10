package edu.cidemags.integrity_core.auth.domain.service;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;

import java.util.UUID;

public interface TokenProvider {

    String generateAccessToken(User user);

    AccessTokenClaims validateAccessToken(String token);

    record AccessTokenClaims(UUID userId, String email, Role role) {
    }
}