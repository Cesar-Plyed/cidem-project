package edu.cidemags.integrity_core.auth.domain.exception;

public class DuplicatePhoneNumberException extends DomainException {
    public DuplicatePhoneNumberException(String phoneNumber) {
        super("El numero de telefono " + phoneNumber + " ya esta registrado para este usuario");
    }
}