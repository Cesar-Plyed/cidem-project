package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
public interface IntegritySnapshotJpaRepository extends JpaRepository<IntegritySnapshotEntity, UUID> {

    Optional<IntegritySnapshotEntity> findByAttemptId(UUID attemptId);
    List<IntegritySnapshotEntity> findByExamSessionId(UUID examSessionId);




    
} 
