package edu.cidemags.integrity_core.auth.domain.exception;

public class DomainException extends RuntimeException{
    protected DomainException (String message){
        super(message);
    }
}
