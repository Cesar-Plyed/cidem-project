package edu.cidemags.integrity_core.auth.domain.exception;

import java.util.UUID;

public class CountryNotFoundException extends DomainException{

    public CountryNotFoundException(UUID countryId) {
        super("No existe el país con id: " +  countryId);
    }
}
