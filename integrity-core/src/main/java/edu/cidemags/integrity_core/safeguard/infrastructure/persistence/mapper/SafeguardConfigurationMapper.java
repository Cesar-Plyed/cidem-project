package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardConfigurationEntity;

/**
 * Mappers manuales (el repo tiene MapStruct en el pom pero ningún mapper lo usa).
 * updateEntity escribe solo campos de negocio: id, timestamps y version los conserva
 * la entidad gestionada para no romper el control optimista.
 */
@Component
public class SafeguardConfigurationMapper {

    public SafeguardConfiguration toDomain(SafeguardConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }
        SafeguardConfiguration domain = new SafeguardConfiguration();
        domain.setId(entity.getId());
        domain.setExamId(entity.getExamId());
        domain.setEnabled(entity.isEnabled());
        domain.setRequireCamera(entity.isRequireCamera());
        domain.setRequireMicrophone(entity.isRequireMicrophone());
        domain.setRequireFullscreen(entity.isRequireFullscreen());
        domain.setBlockCopyPaste(entity.isBlockCopyPaste());
        domain.setMaxTabSwitches(entity.getMaxTabSwitches());
        domain.setRiskThreshold(entity.getRiskThreshold());
        domain.setAutoTerminateOnThreshold(entity.isAutoTerminateOnThreshold());
        domain.setAllowExternalApps(entity.isAllowExternalApps());
        domain.setAllowExternalTabs(entity.isAllowExternalTabs());
        return domain;
    }

    /** Para instancias nuevas (id == null); JPA asigna id, timestamps y version. */
    public SafeguardConfigurationEntity toEntity(SafeguardConfiguration domain) {
        if (domain == null) {
            return null;
        }
        SafeguardConfigurationEntity entity = new SafeguardConfigurationEntity();
        updateEntity(domain, entity);
        return entity;
    }

    /** Copia los campos de negocio de domain sobre la instancia gestionada entity. */
    public void updateEntity(SafeguardConfiguration domain, SafeguardConfigurationEntity entity) {
        entity.setExamId(domain.getExamId());
        entity.setEnabled(domain.isEnabled());
        entity.setRequireCamera(domain.isRequireCamera());
        entity.setRequireMicrophone(domain.isRequireMicrophone());
        entity.setRequireFullscreen(domain.isRequireFullscreen());
        entity.setBlockCopyPaste(domain.isBlockCopyPaste());
        entity.setMaxTabSwitches(domain.getMaxTabSwitches());
        entity.setRiskThreshold(domain.getRiskThreshold());
        entity.setAutoTerminateOnThreshold(domain.isAutoTerminateOnThreshold());
        entity.setAllowExternalApps(domain.isAllowExternalApps());
        entity.setAllowExternalTabs(domain.isAllowExternalTabs());
    }
}
