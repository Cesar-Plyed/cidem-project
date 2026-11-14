package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.Dpia;
import edu.cidemags.integrity_core.legal.domain.model.DPIAStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DpiaRepository extends JpaRepository<Dpia, UUID> {

    List<Dpia> findByStatusOrderByReviewedAtDesc(DPIAStatus status);
}
