package edu.cidemags.integrity_core.auth.infrastructure.persistence.port;

import edu.cidemags.integrity_core.auth.infrastructure.persistence.entity.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CountryJpaRepository extends JpaRepository<CountryEntity, UUID> {

    Optional<CountryEntity> findByIsoCode(String isoCode);

}
