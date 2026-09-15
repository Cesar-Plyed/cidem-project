package edu.cidemags.integrity_core.auth.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.auth.domain.model.RegistrationCode;
import edu.cidemags.integrity_core.auth.domain.port.RegistrationCodeRepository;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.RegistrationCodeEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.RegistrationCodeMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.RegistrationCodeJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RegistrationCodeRepositoryImpl implements RegistrationCodeRepository {

    private final RegistrationCodeJpaRepository jpaRepository;
    private final RegistrationCodeMapper mapper;

    public RegistrationCodeRepositoryImpl(RegistrationCodeJpaRepository jpaRepository, RegistrationCodeMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public RegistrationCode save(RegistrationCode registrationCode) {
        RegistrationCodeEntity entity = mapper.toEntity(registrationCode);
        RegistrationCodeEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RegistrationCode> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }
}