package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.RestrictionRequest;
import edu.cidemags.integrity_core.legal.domain.model.RestrictionStatus;
import edu.cidemags.integrity_core.legal.domain.model.RestrictionType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.RestrictionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestrictionServiceTest {

    @Mock
    private RestrictionRequestRepository repository;

    @InjectMocks
    private RestrictionService service;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void submitRestrictionCreatesActiveRequest() {
        when(repository.save(any(RestrictionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RestrictionRequest request = service.submitRestriction(studentId,
                RestrictionType.PROCESSING, "Revisando datos incorrectos");

        verify(repository).save(any(RestrictionRequest.class));
        assertThat(request.getStatus()).isEqualTo(RestrictionStatus.ACTIVE);
        assertThat(request.getExpiresAt()).isNotNull();
        assertThat(request.isActive()).isTrue();
    }

    @Test
    void revokeRestrictionUpdatesStatus() {
        RestrictionRequest stored = new RestrictionRequest(studentId, RestrictionType.STORAGE,
                "motivo", null);
        when(repository.findById(any())).thenReturn(Optional.of(stored));
        when(repository.save(any(RestrictionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        RestrictionRequest revoked = service.revokeRestriction(UUID.randomUUID());

        assertThat(revoked.getStatus()).isEqualTo(RestrictionStatus.REVOKED);
    }

    @Test
    void isRestrictedWhenActiveRequestExists() {
        RestrictionRequest active = new RestrictionRequest(studentId, RestrictionType.PROCESSING,
                "motivo", java.time.Instant.now().plusSeconds(3600));
        when(repository.findFirstByStudentIdAndStatusOrderByRequestedAtDesc(
                studentId, RestrictionStatus.ACTIVE)).thenReturn(Optional.of(active));

        assertThat(service.isRestricted(studentId)).isTrue();
        assertThat(service.getStatus(studentId).restricted()).isTrue();
    }

    @Test
    void expiredRestrictionDoesNotLimitProcessing() {
        RestrictionRequest expired = new RestrictionRequest(studentId, RestrictionType.PROCESSING,
                "motivo", java.time.Instant.now().minusSeconds(1));
        when(repository.findFirstByStudentIdAndStatusOrderByRequestedAtDesc(
                studentId, RestrictionStatus.ACTIVE)).thenReturn(Optional.of(expired));

        assertThat(service.isRestricted(studentId)).isFalse();
    }
}
