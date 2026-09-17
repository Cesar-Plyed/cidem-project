package edu.cidemags.integrity_core.auth.infrastructure.web.dto;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String firstNames,

        @NotBlank(message = "El apellido es obligatorio")
        String lastNames,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String password,

        @NotNull(message = "El pais es obligatorio")
        UUID countryId,

        String phoneNumber,

        @NotNull(message = "El rol es obligatorio (ADMINISTRATIVO, PROFESOR o ALUMNO)")
        Role role,

        String registrationCode
) {
}