package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardConfigurationEntity;

public interface SafeguardConfigurationJpaRepository extends JpaRepository<SafeguardConfigurationEntity, UUID> {

    Optional<SafeguardConfigurationEntity> findByExamId(UUID examId);
}
