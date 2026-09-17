package edu.cidemags.integrity_core.auth.domain.exception;

import java.util.UUID;

public class UserNotFoundException extends DomainException{
    public UserNotFoundException(UUID userId) {
        super("No existe el usario con el id: " + userId);
    }
}
