package edu.cidemags.integrity_core.auth.domain.exception;

public class InvalidCredentialsException extends DomainException{

    public InvalidCredentialsException() {
        super("Correo o contraseña invalidos");
    }
}
