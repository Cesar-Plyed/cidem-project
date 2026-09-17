package edu.cidemags.integrity_core.auth.domain.port;

import java.util.Optional;

import edu.cidemags.integrity_core.auth.domain.model.RegistrationCode;

public interface RegistrationCodeRepository {

    RegistrationCode save(RegistrationCode registrationCode);

    Optional<RegistrationCode> findByCode(String code);
    
}
