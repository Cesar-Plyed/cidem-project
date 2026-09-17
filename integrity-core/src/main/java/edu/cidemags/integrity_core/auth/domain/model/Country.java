package edu.cidemags.integrity_core.auth.domain.model;

import java.util.UUID;

public record Country(
        UUID id,
        String name,
        String isoCode
) {

    public Country{
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("El pais no puede estar vacio");
        }
        if (isoCode == null || isoCode.length() > 3){
            throw new IllegalArgumentException("El codigo iso debe ser valido (ej. MEX, COL)");
        }
    }

}
