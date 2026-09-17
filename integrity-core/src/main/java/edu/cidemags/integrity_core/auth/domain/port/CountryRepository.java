package edu.cidemags.integrity_core.auth.domain.port;

import edu.cidemags.integrity_core.auth.domain.model.Country;

import java.util.Optional;
import java.util.UUID;

public interface CountryRepository {
    Optional<Country> findById(UUID id);
    Optional<Country> findByIsoCode(String isoCode);
}