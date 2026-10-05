package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardSessionEntity;

/**
 * Mappers manuales. La relación con la configuración la resuelve el adapter
 * (entidad gestionada o proxy por id) y se pasa como parámetro.
 */
@Component
public class SafeguardSessionMapper {

    public SafeguardSession toDomain(SafeguardSessionEntity entity, SafeguardConfiguration configuration) {
        if (entity == null) {
            return null;
        }
        SafeguardSession domain = new SafeguardSession();
        domain.setId(entity.getId());
        domain.setConfiguration(configuration);
        domain.setExamId(entity.getExamId());
        domain.setAttemptId(entity.getAttemptId());
        domain.setStudentId(entity.getStudentId());
        domain.setStatus(entity.getStatus());
        domain.setRiskScore(entity.getRiskScore());
        domain.setStartedAt(entity.getStartedAt());
        domain.setEndedAt(entity.getEndedAt());
        domain.setClientIp(entity.getClientIp());
        domain.setUserAgent(entity.getUserAgent());
        domain.setLastHeartbeatAt(entity.getLastHeartbeatAt());
        return domain;
    }

    /**
     * Copia campos de negocio sobre la instancia gestionada. No toca configuration,
     * createdAt, updatedAt ni version: esos campos son responsabilidad de la entidad/JPA.
     */
    public void updateEntity(SafeguardSession domain, SafeguardSessionEntity entity) {
        entity.setExamId(domain.getExamId());
        entity.setAttemptId(domain.getAttemptId());
        entity.setStudentId(domain.getStudentId());
        entity.setStatus(domain.getStatus());
        entity.setRiskScore(domain.getRiskScore());
        entity.setStartedAt(domain.getStartedAt());
        entity.setEndedAt(domain.getEndedAt());
        entity.setClientIp(domain.getClientIp());
        entity.setUserAgent(domain.getUserAgent());
        entity.setLastHeartbeatAt(domain.getLastHeartbeatAt());
    }
}
