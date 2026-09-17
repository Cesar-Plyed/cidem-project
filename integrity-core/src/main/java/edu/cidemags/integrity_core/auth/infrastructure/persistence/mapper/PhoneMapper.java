package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.Phone;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.PhoneEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class PhoneMapper {

    public Phone toDomain(PhoneEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Phone(entity.getId(), entity.getPhoneNumber(), entity.isPrimary());
    }

    public PhoneEntity toEntity(Phone domain, UserEntity owner) {
        if (domain == null) {
            return null;
        }
        return new PhoneEntity(domain.getId(), owner, domain.getNumber(), domain.isPrimary());
    }
}
