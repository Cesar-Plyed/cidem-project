package edu.cidemags.integrity_core.auth.infrastructure.web.dto;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import jakarta.validation.constraints.NotNull;

public record GenerateRegistrationCodeRequest(

    @NotNull(message = "El rol es obligatorio (PROFESOR o ADMINISTRATIVO)") Role role,

    Integer expiresInDays) {
}