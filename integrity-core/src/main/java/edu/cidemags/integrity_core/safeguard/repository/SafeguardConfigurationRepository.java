package edu.cidemags.integrity_core.safeguard.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cidemags.integrity_core.safeguard.domain.SafeguardConfiguration;

public interface SafeguardConfigurationRepository extends JpaRepository<SafeguardConfiguration, UUID> {

    Optional<SafeguardConfiguration> findByExamId(UUID examId);
}