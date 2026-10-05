package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardConfigurationEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SafeguardConfigurationMapperTest {

    private final SafeguardConfigurationMapper mapper = new SafeguardConfigurationMapper();

    private static SafeguardConfigurationEntity fullEntity() {
        SafeguardConfigurationEntity entity = new SafeguardConfigurationEntity();
        entity.setId(UUID.randomUUID());
        entity.setExamId(UUID.randomUUID());
        entity.setEnabled(true);
        entity.setRequireCamera(true);
        entity.setRequireMicrophone(false);
        entity.setRequireFullscreen(true);
        entity.setBlockCopyPaste(true);
        entity.setMaxTabSwitches(5);
        entity.setRiskThreshold(80);
        entity.setAutoTerminateOnThreshold(true);
        entity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setVersion(7L);
        return entity;
    }

    @Test
    void toDomain_should_map_all_fields_correctly() {
        SafeguardConfigurationEntity entity = fullEntity();

        SafeguardConfiguration domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getExamId()).isEqualTo(entity.getExamId());
        assertThat(domain.isEnabled()).isTrue();
        assertThat(domain.isRequireCamera()).isTrue();
        assertThat(domain.isRequireMicrophone()).isFalse();
        assertThat(domain.isRequireFullscreen()).isTrue();
        assertThat(domain.isBlockCopyPaste()).isTrue();
        assertThat(domain.getMaxTabSwitches()).isEqualTo(5);
        assertThat(domain.getRiskThreshold()).isEqualTo(80);
        assertThat(domain.isAutoTerminateOnThreshold()).isTrue();
    }

    @Test
    void toDomain_should_return_null_when_entity_is_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_should_map_business_fields_and_leave_id_to_jpa() {
        SafeguardConfiguration domain = new SafeguardConfiguration();
        domain.setExamId(UUID.randomUUID());
        domain.setMaxTabSwitches(3);

        SafeguardConfigurationEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isNull(); // la genera JPA
        assertThat(entity.getExamId()).isEqualTo(domain.getExamId());
        assertThat(entity.getMaxTabSwitches()).isEqualTo(3);
    }

    @Test
    void toEntity_should_return_null_when_domain_is_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void updateEntity_should_write_business_fields_but_never_id_createdAt_or_version() {
        // Contrato de concurrencia: sobre la instancia gestionada solo se copian campos
        // de negocio; id/createdAt/version los conserva JPA (@PrePersist/@Version).
        SafeguardConfigurationEntity managed = fullEntity();
        UUID originalId = managed.getId();
        Instant originalCreatedAt = managed.getCreatedAt();
        Instant originalUpdatedAt = managed.getUpdatedAt();
        long originalVersion = managed.getVersion();

        SafeguardConfiguration domain = new SafeguardConfiguration();
        domain.setExamId(managed.getExamId());
        domain.setEnabled(false);
        domain.setMaxTabSwitches(9);
        domain.setRiskThreshold(25);

        mapper.updateEntity(domain, managed);

        assertThat(managed.isEnabled()).isFalse();
        assertThat(managed.getMaxTabSwitches()).isEqualTo(9);
        assertThat(managed.getRiskThreshold()).isEqualTo(25);

        assertThat(managed.getId()).isEqualTo(originalId);
        assertThat(managed.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(managed.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        assertThat(managed.getVersion()).isEqualTo(originalVersion);
    }
}
