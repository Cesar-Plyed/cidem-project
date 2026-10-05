package edu.cidemags.integrity_core.safeguard.domain.port;

import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;

public interface SafeguardConfigurationRepository {

    Optional<SafeguardConfiguration> findByExamId(UUID examId);

    SafeguardConfiguration saveAndFlush(SafeguardConfiguration configuration);
}
