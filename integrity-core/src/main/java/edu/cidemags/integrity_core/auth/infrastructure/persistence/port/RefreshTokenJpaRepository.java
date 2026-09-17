package edu.cidemags.integrity_core.auth.infrastructure.persistence.port;

import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revoked = true where t.userId = :userId and t.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);

}
