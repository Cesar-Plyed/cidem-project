package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardConfigurationEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardSessionEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SafeguardSessionMapperTest {

    private final SafeguardSessionMapper mapper = new SafeguardSessionMapper();

    private static SafeguardSessionEntity fullEntity() {
        SafeguardSessionEntity entity = new SafeguardSessionEntity();
        entity.setId(UUID.randomUUID());
        entity.setExamId(UUID.randomUUID());
        entity.setAttemptId(UUID.randomUUID());
        entity.setStudentId(UUID.randomUUID());
        entity.setStatus(SafeguardSessionStatus.ACTIVE);
        entity.setRiskScore(15);
        entity.setStartedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setClientIp("127.0.0.1");
        entity.setUserAgent("JUnit-UA");
        entity.setLastHeartbeatAt(Instant.parse("2026-01-01T00:01:00Z"));
        return entity;
    }

    private static SafeguardConfiguration configuration(UUID id, UUID examId) {
        SafeguardConfiguration config = new SafeguardConfiguration();
        config.setId(id);
        config.setExamId(examId);
        return config;
    }

    @Test
    void toDomain_should_map_all_fields_and_bind_the_given_configuration() {
        SafeguardSessionEntity entity = fullEntity();
        UUID examId = UUID.randomUUID();
        SafeguardConfiguration config = configuration(UUID.randomUUID(), examId);

        SafeguardSession domain = mapper.toDomain(entity, config);

        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getExamId()).isEqualTo(entity.getExamId());
        assertThat(domain.getAttemptId()).isEqualTo(entity.getAttemptId());
        assertThat(domain.getStudentId()).isEqualTo(entity.getStudentId());
        assertThat(domain.getStatus()).isEqualTo(SafeguardSessionStatus.ACTIVE);
        assertThat(domain.getRiskScore()).isEqualTo(15);
        assertThat(domain.getStartedAt()).isEqualTo(entity.getStartedAt());
        assertThat(domain.getClientIp()).isEqualTo("127.0.0.1");
        assertThat(domain.getUserAgent()).isEqualTo("JUnit-UA");
        assertThat(domain.getLastHeartbeatAt()).isEqualTo(entity.getLastHeartbeatAt());
        assertThat(domain.getConfiguration()).isSameAs(config);
        assertThat(domain.getConfiguration().getExamId()).isEqualTo(examId);
    }

    @Test
    void toDomain_should_return_null_when_entity_is_null() {
        assertThat(mapper.toDomain(null, null)).isNull();
    }

    @Test
    void updateEntity_should_write_business_fields_but_keep_id_timestamps_version_and_configuration() {
        // Contrato de concurrencia: sobre la instancia gestionada solo campos de negocio.
        // La relación con la configuración la resuelve el adapter (proxy por id).
        SafeguardSessionEntity managed = fullEntity();
        SafeguardConfigurationEntity originalConfig = new SafeguardConfigurationEntity();
        originalConfig.setId(UUID.randomUUID());
        managed.setConfiguration(originalConfig);
        UUID originalId = managed.getId();

        SafeguardSession domain = new SafeguardSession();
        domain.setExamId(managed.getExamId());
        domain.setStatus(SafeguardSessionStatus.FLAGGED);
        domain.setRiskScore(99);
        domain.setClientIp("10.0.0.1");

        mapper.updateEntity(domain, managed);

        assertThat(managed.getStatus()).isEqualTo(SafeguardSessionStatus.FLAGGED);
        assertThat(managed.getRiskScore()).isEqualTo(99);
        assertThat(managed.getClientIp()).isEqualTo("10.0.0.1");

        assertThat(managed.getId()).isEqualTo(originalId);
        assertThat(managed.getConfiguration()).isSameAs(originalConfig);
        assertThat(ReflectionTestUtils.getField(managed, "createdAt")).isNull(); // @PrePersist lo llena
        assertThat(ReflectionTestUtils.getField(managed, "version")).isEqualTo(0L);
    }
}
