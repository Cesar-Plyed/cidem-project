package edu.cidemags.integrity_core.auth.infrastructure.security;

import edu.cidemags.integrity_core.auth.domain.model.RegistrationCode;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.port.RegistrationCodeRepository;
import edu.cidemags.integrity_core.auth.domain.service.RegistrationCodeValidator;
import org.springframework.beans.factory.annotation.Value;

public class DbRegistrationCodeValidator implements RegistrationCodeValidator {

    private final RegistrationCodeRepository registrationCodeRepository;
    private final String adminCode;
    private final String professorCode;

    public DbRegistrationCodeValidator(
            RegistrationCodeRepository registrationCodeRepository,
            @Value("${security.registration-codes.administrativo:}") String adminCode,
            @Value("${security.registration-codes.profesor:}") String professorCode) {
        this.registrationCodeRepository = registrationCodeRepository;
        this.adminCode = adminCode;
        this.professorCode = professorCode;
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
        if (providedCode == null || providedCode.isBlank()) {
            return false;
        }

        if (matchesConfiguredCode(role, providedCode)) {
            return true;
        }

        return registrationCodeRepository.findByCode(providedCode)
                .filter(rc -> rc.getRole() == role)
                .filter(RegistrationCode::isValid)
                .isPresent();
    }

    @Override
    public void consume(String providedCode) {
        if (providedCode == null || providedCode.isBlank()) {
            return;
        }

        if (matchesConfiguredCodeForAnyRole(providedCode)) {
            return;
        }

        registrationCodeRepository.findByCode(providedCode).ifPresent(rc -> {
            rc.markUsed();
            registrationCodeRepository.save(rc);
        });
    }

    private boolean matchesConfiguredCode(Role role, String providedCode) {
        return switch (role) {
            case ADMINISTRATIVO -> providedCode.equals(adminCode);
            case PROFESOR -> providedCode.equals(professorCode);
            case ALUMNO -> false;
        };
    }

    private boolean matchesConfiguredCodeForAnyRole(String providedCode) {
        return providedCode.equals(adminCode) || providedCode.equals(professorCode);
    }

}
