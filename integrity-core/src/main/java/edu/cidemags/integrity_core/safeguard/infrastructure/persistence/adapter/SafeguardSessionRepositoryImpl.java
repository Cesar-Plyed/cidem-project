package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardSessionRepository;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardSessionEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardConfigurationMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardSessionMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardConfigurationJpaRepository;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardSessionJpaRepository;

@Repository
public class SafeguardSessionRepositoryImpl implements SafeguardSessionRepository {

    private final SafeguardSessionJpaRepository jpaRepository;
    private final SafeguardConfigurationJpaRepository configurationJpaRepository;
    private final SafeguardSessionMapper mapper;
    private final SafeguardConfigurationMapper configurationMapper;

    public SafeguardSessionRepositoryImpl(SafeguardSessionJpaRepository jpaRepository,
                                          SafeguardConfigurationJpaRepository configurationJpaRepository,
                                          SafeguardSessionMapper mapper,
                                          SafeguardConfigurationMapper configurationMapper) {
        this.jpaRepository = jpaRepository;
        this.configurationJpaRepository = configurationJpaRepository;
        this.mapper = mapper;
        this.configurationMapper = configurationMapper;
    }

    @Override
    public Optional<SafeguardSession> findByAttemptId(UUID attemptId) {
        return jpaRepository.findByAttemptId(attemptId).map(this::toDomain);
    }

    @Override
    public Optional<SafeguardSession> findByIdForUpdate(UUID id) {
        // El bloqueo pesimista queda en la fila JPA; el dominio recibe una instantánea.
        return jpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public SafeguardSession saveAndFlush(SafeguardSession session) {
        // id == null => instancia nueva; si no, se reutiliza la instancia gestionada del
        // contexto de persistencia (mismo thread/tx => caché de primer nivel) para que el
        // UPDATE conserve id/createdAt/version y el bloqueo pesimista siga siendo válido.
        SafeguardSessionEntity entity;
        if (session.getId() == null) {
            entity = new SafeguardSessionEntity();
        } else {
            entity = jpaRepository.findById(session.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "SafeguardSession no encontrada: " + session.getId()));
        }
        mapper.updateEntity(session, entity);

        if (session.getConfiguration() != null) {
            if (session.getConfiguration().getId() != null) {
                // proxy por id: no fuerza una carga y evita pasar una entidad desacoplada
                entity.setConfiguration(configurationJpaRepository.getReferenceById(
                        session.getConfiguration().getId()));
            } else {
                entity.setConfiguration(configurationMapper.toEntity(session.getConfiguration()));
            }
        }
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    private SafeguardSession toDomain(SafeguardSessionEntity entity) {
        SafeguardConfiguration configuration = entity.getConfiguration() == null
                ? null
                : configurationMapper.toDomain(entity.getConfiguration());
        return mapper.toDomain(entity, configuration);
    }
}
