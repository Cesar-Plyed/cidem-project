package edu.cidemags.integrity_core.auth.infrastructure.web.dto;

import java.util.UUID;

import edu.cidemags.integrity_core.auth.domain.model.Country;

public record CountryResponse(UUID id, String name, String isoCode) {
    public static CountryResponse from(Country country) {
        return new CountryResponse(country.id(), country.name(), country.isoCode());
    }
}