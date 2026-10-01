package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardEventEntity;

/**
 * Mappers manuales. La relación con la sesión la resuelve el adapter
 * y se pasa como parámetro.
 */
@Component
public class SafeguardEventMapper {

    public SafeguardEvent toDomain(SafeguardEventEntity entity, SafeguardSession session) {
        if (entity == null) {
            return null;
        }
        SafeguardEvent domain = new SafeguardEvent();
        domain.setId(entity.getId());
        domain.setSession(session);
        domain.setEventId(entity.getEventId());
        domain.setEventType(entity.getEventType());
        domain.setSeverity(entity.getSeverity());
        domain.setRiskPoints(entity.getRiskPoints());
        domain.setOccurredAt(entity.getOccurredAt());
        domain.setMetadata(entity.getMetadata());
        return domain;
    }

    /** Para instancias nuevas (id == null); createdAt lo fija @PrePersist en la entidad. */
    public SafeguardEventEntity toEntity(SafeguardEvent domain) {
        if (domain == null) {
            return null;
        }
        SafeguardEventEntity entity = new SafeguardEventEntity();
        updateEntity(domain, entity);
        return entity;
    }

    /**
     * Copia los campos de negocio sobre la instancia gestionada. No toca session
     * ni createdAt (la sesión la resuelve el adapter; createdAt es de JPA).
     * eventId y eventType son inmutables tras persistir (updatable = false en BD).
     */
    public void updateEntity(SafeguardEvent domain, SafeguardEventEntity entity) {
        entity.setEventId(domain.getEventId());
        entity.setEventType(domain.getEventType());
        entity.setSeverity(domain.getSeverity());
        entity.setRiskPoints(domain.getRiskPoints());
        entity.setOccurredAt(domain.getOccurredAt());
        entity.setMetadata(domain.getMetadata());
    }
}
