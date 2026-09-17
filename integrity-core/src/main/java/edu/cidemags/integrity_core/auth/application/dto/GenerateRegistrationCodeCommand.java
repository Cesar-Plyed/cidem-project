package edu.cidemags.integrity_core.auth.application.dto;

import edu.cidemags.integrity_core.auth.domain.model.Role;

public record GenerateRegistrationCodeCommand(
        Role role,
        Integer expiresInDays) {
}