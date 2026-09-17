package edu.cidemags.integrity_core.auth.domain.exception;

public class EmailAlreadyRegisteredException extends DomainException{

    public EmailAlreadyRegisteredException(String email){
        super("Ya existe un usario registrado con el correo: " + email);
    }

}
