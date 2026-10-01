package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardEventRepository;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardEventEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardSessionEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardConfigurationMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardEventMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardSessionMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardEventJpaRepository;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardSessionJpaRepository;

@Repository
public class SafeguardEventRepositoryImpl implements SafeguardEventRepository {

    private final SafeguardEventJpaRepository jpaRepository;
    private final SafeguardSessionJpaRepository sessionJpaRepository;
    private final SafeguardEventMapper mapper;
    private final SafeguardSessionMapper sessionMapper;
    private final SafeguardConfigurationMapper configurationMapper;

    public SafeguardEventRepositoryImpl(SafeguardEventJpaRepository jpaRepository,
                                        SafeguardSessionJpaRepository sessionJpaRepository,
                                        SafeguardEventMapper mapper,
                                        SafeguardSessionMapper sessionMapper,
                                        SafeguardConfigurationMapper configurationMapper) {
        this.jpaRepository = jpaRepository;
        this.sessionJpaRepository = sessionJpaRepository;
        this.mapper = mapper;
        this.sessionMapper = sessionMapper;
        this.configurationMapper = configurationMapper;
    }

    @Override
    public Optional<SafeguardEvent> findBySessionIdAndEventId(UUID sessionId, UUID eventId) {
        return jpaRepository.findBySessionIdAndEventId(sessionId, eventId).map(this::toDomain);
    }

    @Override
    public Long countBySessionIdAndEventType(UUID sessionId, SafeguardEventType eventType) {
        return jpaRepository.countBySessionIdAndEventType(sessionId, eventType);
    }

    @Override
    public SafeguardEvent saveAndFlush(SafeguardEvent event) {
        // id == null => instancia nueva; si no, instancia gestionada del contexto (ver
        // SafeguardSessionRepositoryImpl): conserva id/createdAt y evita conflictos de versión.
        SafeguardEventEntity entity;
        if (event.getId() == null) {
            entity = new SafeguardEventEntity();
        } else {
            entity = jpaRepository.findById(event.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "SafeguardEvent no encontrado: " + event.getId()));
        }
        mapper.updateEntity(event, entity);

        if (event.getSession() != null && event.getSession().getId() != null) {
            entity.setSession(sessionJpaRepository.getReferenceById(event.getSession().getId()));
        }
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    private SafeguardEvent toDomain(SafeguardEventEntity entity) {
        return mapper.toDomain(entity, resolveSession(entity.getSession()));
    }

    private SafeguardSession resolveSession(SafeguardSessionEntity sessionEntity) {
        if (sessionEntity == null) {
            return null;
        }
        var configuration = sessionEntity.getConfiguration() == null
                ? null
                : configurationMapper.toDomain(sessionEntity.getConfiguration());
        return sessionMapper.toDomain(sessionEntity, configuration);
    }
}
