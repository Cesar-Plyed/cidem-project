package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardConfigurationEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardSessionEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardConfigurationMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardSessionMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardConfigurationJpaRepository;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardSessionJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafeguardSessionRepositoryImplTest {

    @Mock
    private SafeguardSessionJpaRepository jpaRepository;

    @Mock
    private SafeguardConfigurationJpaRepository configurationJpaRepository;

    private final SafeguardSessionMapper mapper = new SafeguardSessionMapper();
    private final SafeguardConfigurationMapper configurationMapper = new SafeguardConfigurationMapper();
    private SafeguardSessionRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SafeguardSessionRepositoryImpl(
                jpaRepository, configurationJpaRepository, mapper, configurationMapper);
    }

    private static SafeguardConfigurationEntity configurationEntity() {
        SafeguardConfigurationEntity config = new SafeguardConfigurationEntity();
        config.setId(UUID.randomUUID());
        config.setExamId(UUID.randomUUID());
        return config;
    }

    private static SafeguardSessionEntity managedEntity(UUID id, SafeguardConfigurationEntity config) {
        SafeguardSessionEntity entity = new SafeguardSessionEntity();
        entity.setId(id);
        entity.setExamId(UUID.randomUUID());
        entity.setAttemptId(UUID.randomUUID());
        entity.setStudentId(UUID.randomUUID());
        entity.setStatus(SafeguardSessionStatus.ACTIVE);
        entity.setRiskScore(15);
        entity.setConfiguration(config);
        // createdAt/version no tienen setter: los fija JPA, aquí se siembran para
        // demostrar que updateEntity no los toca.
        ReflectionTestUtils.setField(entity, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(entity, "version", 4L);
        return entity;
    }

    @Test
    void findByIdForUpdate_should_map_session_with_its_configuration() {
        UUID id = UUID.randomUUID();
        SafeguardConfigurationEntity config = configurationEntity();
        when(jpaRepository.findByIdForUpdate(id)).thenReturn(Optional.of(managedEntity(id, config)));

        Optional<SafeguardSession> result = repository.findByIdForUpdate(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getRiskScore()).isEqualTo(15);
        assertThat(result.get().getConfiguration()).isNotNull();
        assertThat(result.get().getConfiguration().getId()).isEqualTo(config.getId());
    }

    @Test
    void findByIdForUpdate_should_return_empty_when_not_found() {
        when(jpaRepository.findByIdForUpdate(any(UUID.class))).thenReturn(Optional.empty());

        assertThat(repository.findByIdForUpdate(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByAttemptId_should_map_session_when_present() {
        UUID attemptId = UUID.randomUUID();
        SafeguardSessionEntity managed = managedEntity(UUID.randomUUID(), configurationEntity());
        when(jpaRepository.findByAttemptId(attemptId)).thenReturn(Optional.of(managed));

        Optional<SafeguardSession> result = repository.findByAttemptId(attemptId);

        assertThat(result).isPresent();
        assertThat(result.get().getAttemptId()).isEqualTo(managed.getAttemptId());
        assertThat(result.get().getConfiguration()).isNotNull();
    }

    @Test
    void saveAndFlush_newSession_should_createEntityAndResolveConfigurationByReference() {
        // id == null => entidad nueva; la asociación se resuelve con getReferenceById
        // (proxy por id, sin SELECT extra).
        SafeguardConfiguration config = new SafeguardConfiguration();
        config.setId(UUID.randomUUID());
        config.setExamId(UUID.randomUUID());
        SafeguardSession domain = new SafeguardSession();
        domain.setExamId(UUID.randomUUID());
        domain.setAttemptId(UUID.randomUUID());
        domain.setStudentId(UUID.randomUUID());
        domain.setStatus(SafeguardSessionStatus.ACTIVE);
        domain.setConfiguration(config);
        SafeguardConfigurationEntity configProxy = configurationEntity();
        when(configurationJpaRepository.getReferenceById(config.getId())).thenReturn(configProxy);
        when(jpaRepository.saveAndFlush(any(SafeguardSessionEntity.class))).thenAnswer(invocation -> {
            SafeguardSessionEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", UUID.randomUUID()); // lo asignaría JPA
            return entity;
        });

        SafeguardSession result = repository.saveAndFlush(domain);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getConfiguration()).isNotNull();
        assertThat(result.getConfiguration().getId()).isEqualTo(configProxy.getId());
        verify(configurationJpaRepository).getReferenceById(config.getId());
        verify(jpaRepository, never()).findById(any(UUID.class));
    }

    @Test
    void saveAndFlush_newSession_withoutConfigurationId_should_embedEntityDirectly() {
        // Rama alternativa: configuración sin id (alta conjunta) => se persiste la
        // entidad de configuración embebida en lugar de un proxy por id.
        SafeguardConfiguration config = new SafeguardConfiguration();
        config.setExamId(UUID.randomUUID());
        SafeguardSession domain = new SafeguardSession();
        domain.setExamId(UUID.randomUUID());
        domain.setAttemptId(UUID.randomUUID());
        domain.setStudentId(UUID.randomUUID());
        domain.setStatus(SafeguardSessionStatus.ACTIVE);
        domain.setConfiguration(config);
        when(jpaRepository.saveAndFlush(any(SafeguardSessionEntity.class))).thenAnswer(invocation -> {
            SafeguardSessionEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
            return entity;
        });

        SafeguardSession result = repository.saveAndFlush(domain);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getConfiguration()).isNotNull();
        assertThat(result.getConfiguration().getExamId()).isEqualTo(config.getExamId());
        verify(configurationJpaRepository, never()).getReferenceById(any(UUID.class));
    }

    @Test
    void saveAndFlush_existingInstance_should_reuseManagedRowAndKeepIdTimestampsVersion() {
        SafeguardConfigurationEntity config = configurationEntity();
        SafeguardSessionEntity managed = managedEntity(UUID.randomUUID(), config);
        SafeguardSession domain = mapper.toDomain(managed, configurationMapper.toDomain(config));
        domain.setRiskScore(55);
        domain.setStatus(SafeguardSessionStatus.FLAGGED);
        when(jpaRepository.findById(managed.getId())).thenReturn(Optional.of(managed));
        when(configurationJpaRepository.getReferenceById(config.getId())).thenReturn(config);
        when(jpaRepository.saveAndFlush(managed)).thenReturn(managed);

        SafeguardSession result = repository.saveAndFlush(domain);

        assertThat(result.getId()).isEqualTo(managed.getId());
        assertThat(managed.getRiskScore()).isEqualTo(55);
        assertThat(managed.getStatus()).isEqualTo(SafeguardSessionStatus.FLAGGED);
        assertThat(ReflectionTestUtils.getField(managed, "createdAt"))
                .isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(ReflectionTestUtils.getField(managed, "version")).isEqualTo(4L);
        assertThat(managed.getConfiguration()).isSameAs(config);
        verify(jpaRepository).saveAndFlush(managed);
    }

    @Test
    void saveAndFlush_existingIdWithoutManagedRow_should_throwIllegalState() {
        UUID missingId = UUID.randomUUID();
        SafeguardSession domain = new SafeguardSession();
        domain.setId(missingId);
        when(jpaRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.saveAndFlush(domain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(missingId.toString());
    }
}
