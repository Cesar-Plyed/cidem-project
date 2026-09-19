package edu.cidemags.integrity_core.auth.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.auth.domain.model.Country;

public interface CountryRepository {
    List<Country> findAll();
    Optional<Country> findById(UUID id);
    Optional<Country> findByIsoCode(String isoCode);
}