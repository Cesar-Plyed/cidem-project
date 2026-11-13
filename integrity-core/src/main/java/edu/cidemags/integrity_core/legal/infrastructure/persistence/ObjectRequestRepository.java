package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.ObjectRequest;
import edu.cidemags.integrity_core.legal.domain.model.ObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjectRequestRepository extends JpaRepository<ObjectRequest, UUID> {

    List<ObjectRequest> findByStudentIdOrderByRequestedAtDesc(UUID studentId);

    Optional<ObjectRequest> findFirstByStudentIdAndStatusInOrderByRequestedAtDesc(
            UUID studentId, List<ObjectStatus> statuses);

    Optional<ObjectRequest> findFirstByStudentIdOrderByRequestedAtDesc(UUID studentId);
}
