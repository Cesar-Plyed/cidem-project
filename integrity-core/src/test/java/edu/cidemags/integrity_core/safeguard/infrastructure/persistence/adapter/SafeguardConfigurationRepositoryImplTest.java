package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardConfiguration;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardConfigurationEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardConfigurationMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardConfigurationJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class SafeguardConfigurationRepositoryImplTest {

    @Mock
    private SafeguardConfigurationJpaRepository jpaRepository;

    private final SafeguardConfigurationMapper mapper = new SafeguardConfigurationMapper();
    private SafeguardConfigurationRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SafeguardConfigurationRepositoryImpl(jpaRepository, mapper);
    }

    private static SafeguardConfigurationEntity managedEntity(UUID id) {
        SafeguardConfigurationEntity entity = new SafeguardConfigurationEntity();
        entity.setId(id);
        entity.setExamId(UUID.randomUUID());
        entity.setMaxTabSwitches(3);
        entity.setRiskThreshold(60);
        entity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setVersion(7L);
        return entity;
    }

    @Test
    void findByExamId_should_map_to_domain_when_present() {
        UUID examId = UUID.randomUUID();
        SafeguardConfigurationEntity entity = managedEntity(UUID.randomUUID());
        when(jpaRepository.findByExamId(examId)).thenReturn(Optional.of(entity));

        Optional<SafeguardConfiguration> result = repository.findByExamId(examId);

        assertThat(result).isPresent();
        assertThat(result.get().getExamId()).isEqualTo(entity.getExamId());
        assertThat(result.get().getMaxTabSwitches()).isEqualTo(3);
        assertThat(result.get().getRiskThreshold()).isEqualTo(60);
    }

    @Test
    void findByExamId_should_return_empty_when_not_found() {
        when(jpaRepository.findByExamId(any(UUID.class))).thenReturn(Optional.empty());

        assertThat(repository.findByExamId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void saveAndFlush_newInstance_should_persist_without_managed_lookup() {
        // id == null => rama nueva: nunca se busca instancia gestionada.
        SafeguardConfiguration domain = new SafeguardConfiguration();
        domain.setExamId(UUID.randomUUID());
        domain.setMaxTabSwitches(5);
        when(jpaRepository.saveAndFlush(any(SafeguardConfigurationEntity.class))).thenAnswer(invocation -> {
            SafeguardConfigurationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID()); // lo asignaría JPA
            return entity;
        });

        SafeguardConfiguration result = repository.saveAndFlush(domain);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getMaxTabSwitches()).isEqualTo(5);
        verify(jpaRepository, never()).findById(any(UUID.class));
    }

    @Test
    void saveAndFlush_existingInstance_should_reuseManagedRowAndKeepIdTimestampsVersion() {
        // id != null => rama de actualización sobre la instancia gestionada: el mapper
        // solo copia campos de negocio, así que id/createdAt/version no cambian.
        SafeguardConfigurationEntity managed = managedEntity(UUID.randomUUID());
        SafeguardConfiguration domain = mapper.toDomain(managed);
        domain.setMaxTabSwitches(9);
        domain.setRiskThreshold(25);
        when(jpaRepository.findById(managed.getId())).thenReturn(Optional.of(managed));
        when(jpaRepository.saveAndFlush(managed)).thenReturn(managed);

        SafeguardConfiguration result = repository.saveAndFlush(domain);

        assertThat(result.getId()).isEqualTo(managed.getId());
        assertThat(managed.getMaxTabSwitches()).isEqualTo(9);
        assertThat(managed.getRiskThreshold()).isEqualTo(25);
        assertThat(managed.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(managed.getUpdatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(managed.getVersion()).isEqualTo(7L);
        verify(jpaRepository).saveAndFlush(managed);
    }

    @Test
    void saveAndFlush_existingIdWithoutManagedRow_should_throwIllegalState() {
        UUID missingId = UUID.randomUUID();
        SafeguardConfiguration domain = new SafeguardConfiguration();
        domain.setId(missingId);
        when(jpaRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.saveAndFlush(domain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(missingId.toString());
    }
}
