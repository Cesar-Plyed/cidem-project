package edu.cidemags.integrity_core.auth.infrastructure.web.dto;

import edu.cidemags.integrity_core.auth.domain.model.RegistrationCode;
import edu.cidemags.integrity_core.auth.domain.model.Role;

import java.time.OffsetDateTime;

public record GenerateRegistrationCodeResponse(
        String code,
        Role role,
        OffsetDateTime expiresAt) {
    public static GenerateRegistrationCodeResponse from(RegistrationCode registrationCode) {
        return new GenerateRegistrationCodeResponse(
                registrationCode.getCode(),
                registrationCode.getRole(),
                registrationCode.getExpiresAt());
    }
}