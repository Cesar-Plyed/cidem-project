package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.RegistrationCode;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.RegistrationCodeEntity;
import org.springframework.stereotype.Component;

@Component
public class RegistrationCodeMapper {

    public RegistrationCode toDomain(RegistrationCodeEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RegistrationCode(
                entity.getId(),
                entity.getCode(),
                entity.getRole(),
                entity.isUsed(),
                entity.getExpiresAt(),
                entity.getUsedAt()
        );
    }

    public RegistrationCodeEntity toEntity(RegistrationCode domain) {
        if (domain == null) {
            return null;
        }
        return new RegistrationCodeEntity(
                domain.getId(),
                domain.getCode(),
                domain.getRole(),
                domain.isUsed(),
                domain.getExpiresAt(),
                domain.getUsedAt()
        );
    }
}