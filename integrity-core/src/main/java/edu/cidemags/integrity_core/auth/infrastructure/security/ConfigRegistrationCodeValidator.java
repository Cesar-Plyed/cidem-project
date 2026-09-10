package edu.cidemags.integrity_core.auth.infrastructure.security;

import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.service.RegistrationCodeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ConfigRegistrationCodeValidator implements RegistrationCodeValidator {

    private final String administrativoCode;
    private final String profesorCode;

    public ConfigRegistrationCodeValidator(
            @Value("${security.registration-codes.administrativo}") String administrativoCode,
            @Value("${security.registration-codes.profesor}") String profesorCode) {
        this.administrativoCode = administrativoCode;
        this.profesorCode = profesorCode;
    }

    @Override
    public boolean requiresCode(Role role) {
        return role == Role.ADMINISTRATIVO || role == Role.PROFESOR;
    }

    @Override
    public boolean isValid(Role role, String providedCode) {
        if (!requiresCode(role)) {
            return true;
        }
        if (providedCode == null) {
            return false;
        }

        String expectedCode = switch (role) {
            case ADMINISTRATIVO -> administrativoCode;
            case PROFESOR -> profesorCode;
            case ALUMNO -> null;
        };

        return expectedCode != null && constantTimeEquals(expectedCode, providedCode);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}