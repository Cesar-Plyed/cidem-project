package edu.cidemags.integrity_core.auth.application.dto;

import edu.cidemags.integrity_core.auth.domain.model.Role;

import java.util.UUID;

public record RegisterUserCommand(
        String firstNames,
        String lastNames,
        String email,
        String rawPassword,
        UUID countryId,
        String phoneNumber,
        Role role,
        String registrationCode
) {
}