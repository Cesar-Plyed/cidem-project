package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.ErasureRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErasureRequestRepository extends JpaRepository<ErasureRequest, UUID> {

    List<ErasureRequest> findByStudentIdOrderByRequestedAtDesc(UUID studentId);
}
