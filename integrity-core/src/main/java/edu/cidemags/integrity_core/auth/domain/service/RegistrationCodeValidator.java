package edu.cidemags.integrity_core.auth.domain.service;

import edu.cidemags.integrity_core.auth.domain.model.Role;

public interface RegistrationCodeValidator {

    boolean isValid(Role role, String providedCode);

    boolean requiresCode(Role role);

}