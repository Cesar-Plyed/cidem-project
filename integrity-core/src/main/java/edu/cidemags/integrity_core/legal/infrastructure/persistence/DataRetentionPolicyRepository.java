package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.DataRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, UUID> {

    Optional<DataRetentionPolicy> findFirstByActiveTrueOrderByRetentionStartDateDesc();
}
