package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.DataBreachNotification;
import edu.cidemags.integrity_core.legal.domain.model.BreachStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataBreachNotificationRepository extends JpaRepository<DataBreachNotification, UUID> {

    List<DataBreachNotification> findByStudentIdAndStatusNotOrderByNotifiedAtDesc(
            UUID studentId, BreachStatus status);
}
