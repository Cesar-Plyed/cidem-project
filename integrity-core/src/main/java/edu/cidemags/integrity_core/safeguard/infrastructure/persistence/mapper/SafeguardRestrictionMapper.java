package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardRestrictionEntity;

/** Mapper manual de restricciones (mismo criterio que el resto de mappers de SafeGuard). */
@Component
public class SafeguardRestrictionMapper {

    public Restriction toDomain(SafeguardRestrictionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Restriction(entity.getId(), entity.getType(), entity.isEnabled(),
                entity.getMaxOccurrences(), entity.getWindowSeconds(), entity.getSeverity());
    }

    /** El examen no vive en el dominio: la asociación se pasa explícitamente al persistir. */
    public SafeguardRestrictionEntity toEntity(UUID examId, Restriction restriction) {
        SafeguardRestrictionEntity entity = new SafeguardRestrictionEntity();
        entity.setId(restriction.id());
        entity.setExamId(examId);
        entity.setType(restriction.type());
        entity.setEnabled(restriction.enabled());
        entity.setMaxOccurrences(restriction.maxOccurrences());
        entity.setWindowSeconds(restriction.windowSeconds());
        entity.setSeverity(restriction.severity());
        return entity;
    }
}
