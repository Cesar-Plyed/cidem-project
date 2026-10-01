package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardConfigurationRepository;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardConfigurationEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardConfigurationMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardConfigurationJpaRepository;

@Repository
public class SafeguardConfigurationRepositoryImpl implements SafeguardConfigurationRepository {

    private final SafeguardConfigurationJpaRepository jpaRepository;
    private final SafeguardConfigurationMapper mapper;

    public SafeguardConfigurationRepositoryImpl(SafeguardConfigurationJpaRepository jpaRepository,
                                                SafeguardConfigurationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<SafeguardConfiguration> findByExamId(UUID examId) {
        return jpaRepository.findByExamId(examId).map(mapper::toDomain);
    }

    @Override
    public SafeguardConfiguration saveAndFlush(SafeguardConfiguration configuration) {
        // id == null => instancia nueva; si no, se reutiliza la instancia gestionada del
        // contexto de persistencia para no pisar id/createdAt/version (@Version).
        SafeguardConfigurationEntity entity;
        if (configuration.getId() == null) {
            entity = mapper.toEntity(configuration);
        } else {
            entity = jpaRepository.findById(configuration.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "SafeguardConfiguration no encontrada: " + configuration.getId()));
            mapper.updateEntity(configuration, entity);
        }
        return mapper.toDomain(jpaRepository.saveAndFlush(entity));
    }
}
