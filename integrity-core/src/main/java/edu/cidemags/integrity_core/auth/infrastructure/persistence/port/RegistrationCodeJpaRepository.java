package edu.cidemags.integrity_core.auth.infrastructure.persistence.port;

import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.RegistrationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationCodeJpaRepository extends JpaRepository<RegistrationCodeEntity, UUID> {

    Optional<RegistrationCodeEntity> findByCode(String code);
}