package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardRestrictionEntity;

public interface SafeguardRestrictionJpaRepository extends JpaRepository<SafeguardRestrictionEntity, UUID> {

    List<SafeguardRestrictionEntity> findByExamIdOrderByTypeAsc(UUID examId);

    void deleteByExamId(UUID examId);
}
