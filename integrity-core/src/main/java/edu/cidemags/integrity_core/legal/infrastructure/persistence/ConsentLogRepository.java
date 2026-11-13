package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.ConsentLog;
import edu.cidemags.integrity_core.legal.domain.model.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentLogRepository extends JpaRepository<ConsentLog, UUID> {

    List<ConsentLog> findByStudentIdOrderByCreatedAtAsc(UUID studentId);

    Optional<ConsentLog> findTopByStudentIdAndConsentTypeOrderByCreatedAtDesc(UUID studentId, ConsentType consentType);

    List<ConsentLog> findByStudentIdAndConsentTypeOrderByCreatedAtAsc(UUID studentId, ConsentType consentType);
}
