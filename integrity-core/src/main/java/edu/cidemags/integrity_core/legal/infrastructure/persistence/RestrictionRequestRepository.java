package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.RestrictionRequest;
import edu.cidemags.integrity_core.legal.domain.model.RestrictionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestrictionRequestRepository extends JpaRepository<RestrictionRequest, UUID> {

    List<RestrictionRequest> findByStudentIdOrderByRequestedAtDesc(UUID studentId);

    Optional<RestrictionRequest> findFirstByStudentIdAndStatusOrderByRequestedAtDesc(
            UUID studentId, RestrictionStatus status);
}
