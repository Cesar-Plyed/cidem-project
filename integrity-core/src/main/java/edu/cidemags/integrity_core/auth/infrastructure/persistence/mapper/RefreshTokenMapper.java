package edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper;

import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

    public RefreshToken toDomain(RefreshTokenEntity entity){
        if (entity == null){
            return null;
        }

        return new RefreshToken(entity.getId(), entity.getUserId(),entity.getToken(),entity.getExpiresAt(),entity.isRevoked());
    }

    public RefreshTokenEntity toEntity(RefreshToken domain){
        if (domain == null){
            return null;
        }

        return new RefreshTokenEntity(domain.getId(),domain.getUserId(),domain.getToken(),domain.getExpiresAt(),domain.isRevoked());
    }
}
