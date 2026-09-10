package edu.cidemags.integrity_core.auth.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.auth.domain.model.RefreshToken;
import edu.cidemags.integrity_core.auth.domain.port.RefreshTokenRepository;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import edu.cidemags.integrity_core.auth.infrastructure.persistence.port.RefreshTokenJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    public RefreshTokenRepositoryImpl(RefreshTokenJpaRepository jpaRepository, RefreshTokenMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = mapper.toEntity(refreshToken);
        RefreshTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        jpaRepository.revokeAllByUserId(userId);
    }
}
