package edu.cidemags.integrity_core.auth.domain.exception;

public class InvalidAccessTokenException extends DomainException {
    public InvalidAccessTokenException() {
        super("El access token es invalido, expiro o esta mal formado");
    }
}