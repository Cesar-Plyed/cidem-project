package edu.cidemags.integrity_core.auth.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.auth.domain.model.Country;
import edu.cidemags.integrity_core.auth.domain.port.CountryRepository;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.CountryMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.CountryJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CountryRepositoryImpl implements CountryRepository {

    private final CountryJpaRepository jpaRepository;
    private final CountryMapper mapper;

    public CountryRepositoryImpl(CountryJpaRepository jpaRepository, CountryMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Country> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Country> findByIsoCode(String isoCode) {
        return jpaRepository.findByIsoCode(isoCode).map(mapper::toDomain);
    }
}
