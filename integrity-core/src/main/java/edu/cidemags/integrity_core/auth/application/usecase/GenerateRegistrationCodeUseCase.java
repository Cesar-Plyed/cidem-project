package edu.cidemags.integrity_core.auth.application.usecase;

import edu.cidemags.integrity_core.auth.application.dto.GenerateRegistrationCodeCommand;
import edu.cidemags.integrity_core.auth.domain.model.RegistrationCode;
import edu.cidemags.integrity_core.auth.domain.model.Role;
import edu.cidemags.integrity_core.auth.domain.port.RegistrationCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Service
public class GenerateRegistrationCodeUseCase {

    private final RegistrationCodeRepository registrationCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public GenerateRegistrationCodeUseCase(RegistrationCodeRepository registrationCodeRepository) {
        this.registrationCodeRepository = registrationCodeRepository;
    }

    @Transactional
    public RegistrationCode execute(GenerateRegistrationCodeCommand command) {
        if (command.role() == null || command.role() == Role.ALUMNO) {
            throw new IllegalArgumentException(
                    "Solo se pueden generar codigos de registro para PROFESOR o ADMINISTRATIVO");
        }

        OffsetDateTime expiresAt = command.expiresInDays() != null
                ? OffsetDateTime.now(ZoneId.systemDefault()).plusDays(command.expiresInDays())
                : null;

        RegistrationCode registrationCode = new RegistrationCode(generateRandomCode(), command.role(), expiresAt);
        return registrationCodeRepository.save(registrationCode);
    }

    private String generateRandomCode() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}