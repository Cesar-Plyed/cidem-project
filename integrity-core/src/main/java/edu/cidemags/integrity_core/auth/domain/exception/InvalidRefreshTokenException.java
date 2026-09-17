package edu.cidemags.integrity_core.auth.domain.exception;

public class InvalidRefreshTokenException extends DomainException{

    public InvalidRefreshTokenException() {
        super("El refresh token es invalido, expiro o fue revocado");
    }
}
