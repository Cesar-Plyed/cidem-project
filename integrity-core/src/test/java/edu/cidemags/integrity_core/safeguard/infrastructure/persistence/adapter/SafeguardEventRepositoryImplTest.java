package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEvent;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardEventType;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSession;
import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardConfigurationEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardEventEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardSessionEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardConfigurationMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardEventMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardSessionMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardEventJpaRepository;
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
class SafeguardEventRepositoryImplTest {

    @Mock
    private SafeguardEventJpaRepository jpaRepository;

    @Mock
    private SafeguardSessionJpaRepository sessionJpaRepository;

    private final SafeguardEventMapper mapper = new SafeguardEventMapper();
    private final SafeguardSessionMapper sessionMapper = new SafeguardSessionMapper();
    private final SafeguardConfigurationMapper configurationMapper = new SafeguardConfigurationMapper();
    private SafeguardEventRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SafeguardEventRepositoryImpl(
                jpaRepository, sessionJpaRepository, mapper, sessionMapper, configurationMapper);
    }

    private static SafeguardConfigurationEntity configurationEntity() {
        SafeguardConfigurationEntity config = new SafeguardConfigurationEntity();
        config.setId(UUID.randomUUID());
        config.setExamId(UUID.randomUUID());
        return config;
    }

    private static SafeguardSessionEntity sessionEntity(SafeguardConfigurationEntity config) {
        SafeguardSessionEntity session = new SafeguardSessionEntity();
        session.setId(UUID.randomUUID());
        session.setExamId(UUID.randomUUID());
        session.setAttemptId(UUID.randomUUID());
        session.setStudentId(UUID.randomUUID());
        session.setStatus(SafeguardSessionStatus.ACTIVE);
        session.setConfiguration(config);
        return session;
    }

    private static SafeguardEventEntity eventEntity(SafeguardSessionEntity session) {
        SafeguardEventEntity entity = new SafeguardEventEntity();
        entity.setId(UUID.randomUUID());
        entity.setEventId(UUID.randomUUID());
        entity.setEventType(SafeguardEventType.TAB_SWITCH);
        entity.setSeverity(2);
        entity.setRiskPoints(5);
        entity.setOccurredAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setSession(session);
        return entity;
    }

    @Test
    void findBySessionIdAndEventId_should_map_event_resolving_session_and_configuration() {
        UUID sessionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        SafeguardSessionEntity session = sessionEntity(configurationEntity());
        SafeguardEventEntity entity = eventEntity(session);
        when(jpaRepository.findBySessionIdAndEventId(sessionId, eventId)).thenReturn(Optional.of(entity));

        Optional<SafeguardEvent> result = repository.findBySessionIdAndEventId(sessionId, eventId);

        assertThat(result).isPresent();
        assertThat(result.get().getEventId()).isEqualTo(entity.getEventId());
        assertThat(result.get().getEventType()).isEqualTo(SafeguardEventType.TAB_SWITCH);
        assertThat(result.get().getSession()).isNotNull();
        assertThat(result.get().getSession().getId()).isEqualTo(session.getId());
        assertThat(result.get().getSession().getConfiguration()).isNotNull();
    }

    @Test
    void findBySessionIdAndEventId_should_return_empty_when_not_found() {
        when(jpaRepository.findBySessionIdAndEventId(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThat(repository.findBySessionIdAndEventId(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void countBySessionIdAndEventType_should_delegate_to_jpa() {
        UUID sessionId = UUID.randomUUID();
        when(jpaRepository.countBySessionIdAndEventType(sessionId, SafeguardEventType.TAB_SWITCH)).thenReturn(4L);

        Long count = repository.countBySessionIdAndEventType(sessionId, SafeguardEventType.TAB_SWITCH);

        assertThat(count).isEqualTo(4L);
    }

    @Test
    void saveAndFlush_newEvent_should_createEntityAndResolveSessionByReference() {
        // id == null => entidad nueva; la sesión se asocia con getReferenceById (proxy).
        SafeguardSession session = new SafeguardSession();
        session.setId(UUID.randomUUID());
        SafeguardEvent domain = new SafeguardEvent();
        domain.setEventId(UUID.randomUUID());
        domain.setEventType(SafeguardEventType.COPY_PASTE);
        domain.setSeverity(3);
        domain.setRiskPoints(10);
        domain.setOccurredAt(Instant.now());
        domain.setSession(session);
        SafeguardConfigurationEntity config = configurationEntity();
        SafeguardSessionEntity sessionProxy = sessionEntity(config);
        when(sessionJpaRepository.getReferenceById(session.getId())).thenReturn(sessionProxy);
        when(jpaRepository.saveAndFlush(any(SafeguardEventEntity.class))).thenAnswer(invocation -> {
            SafeguardEventEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", UUID.randomUUID()); // lo asignaría JPA
            return entity;
        });

        SafeguardEvent result = repository.saveAndFlush(domain);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEventType()).isEqualTo(SafeguardEventType.COPY_PASTE);
        assertThat(result.getSession()).isNotNull();
        assertThat(result.getSession().getId()).isEqualTo(sessionProxy.getId());
        verify(sessionJpaRepository).getReferenceById(session.getId());
        verify(jpaRepository, never()).findById(any(UUID.class));
    }

    @Test
    void saveAndFlush_existingInstance_should_reuseManagedRowAndResolveSessionByReference() {
        SafeguardSessionEntity session = sessionEntity(configurationEntity());
        SafeguardEventEntity managed = eventEntity(session);
        SafeguardEvent domain = mapper.toDomain(managed,
                sessionMapper.toDomain(session, configurationMapper.toDomain(session.getConfiguration())));
        domain.setRiskPoints(35);
        when(jpaRepository.findById(managed.getId())).thenReturn(Optional.of(managed));
        when(sessionJpaRepository.getReferenceById(session.getId())).thenReturn(session);
        when(jpaRepository.saveAndFlush(managed)).thenReturn(managed);

        SafeguardEvent result = repository.saveAndFlush(domain);

        assertThat(result.getId()).isEqualTo(managed.getId());
        assertThat(managed.getRiskPoints()).isEqualTo(35);
        assertThat(managed.getSession()).isSameAs(session);
        verify(sessionJpaRepository).getReferenceById(session.getId());
        verify(jpaRepository).saveAndFlush(managed);
    }

    @Test
    void saveAndFlush_existingIdWithoutManagedRow_should_throwIllegalState() {
        UUID missingId = UUID.randomUUID();
        SafeguardEvent domain = new SafeguardEvent();
        domain.setId(missingId);
        when(jpaRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.saveAndFlush(domain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(missingId.toString());
    }
}
