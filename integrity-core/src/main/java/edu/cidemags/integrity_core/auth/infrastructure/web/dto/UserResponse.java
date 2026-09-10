package edu.cidemags.integrity_core.auth.infrastructure.web.dto;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.model.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstNames,
        String lastNames,
        String email,
        String countryName,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstNames(),
                user.getLastNames(),
                user.getEmail(),
                user.getCountry().name(),
                user.getRole()
        );
    }
}