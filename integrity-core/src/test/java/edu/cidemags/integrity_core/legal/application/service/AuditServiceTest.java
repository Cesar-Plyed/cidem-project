package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.AuditLog;
import edu.cidemags.integrity_core.legal.domain.model.ActorRole;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditService service;

    @Test
    void recordPersistsImmutableEntry() {
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        AuditLog log = service.record(actorId, ActorRole.STUDENT, "START_EXAM", "EXAM", entityId,
                Map.of("status", "started"), Map.of("status", "running"), "10.0.0.1", "JUnit");

        assertThat(log.getActorId()).isEqualTo(actorId);
        assertThat(log.getAction()).isEqualTo("START_EXAM");
        assertThat(log.getPreviousState()).isNotEqualTo(log.getCurrentState());
        assertThat(log.getOccurredAt()).isNotNull();
    }

    @Test
    void queryFiltersByBeforeDateAndCapsLimit() {
        Instant before = Instant.parse("2026-01-01T00:00:00Z");
        when(repository.search(any(), any(), any(), any())).thenReturn(List.of());

        service.query(null, before, null, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq(null), eq(null), eq(before), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void queryDefaultsToOneHundredRows() {
        when(repository.search(any(), any(), any(), any())).thenReturn(List.of());

        service.query(UUID.randomUUID(), null, null, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(any(), eq(null), eq(null), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }
}
