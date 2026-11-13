package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.ObjectRequest;
import edu.cidemags.integrity_core.legal.domain.model.ObjectStatus;
import edu.cidemags.integrity_core.legal.domain.model.ObjectType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.ObjectRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectServiceTest {

    @Mock
    private ObjectRequestRepository repository;

    @InjectMocks
    private ObjectService service;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void submitObjectInsertsPendingRequest() {
        when(repository.save(any(ObjectRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ObjectRequest request = service.submitObject(studentId, ObjectType.PROFILING,
                "No quiero perfilado automatizado");

        verify(repository).save(any(ObjectRequest.class));
        assertThat(request.getStatus()).isEqualTo(ObjectStatus.PENDING);
        assertThat(request.getObjectType()).isEqualTo(ObjectType.PROFILING);
    }

    @Test
    void resolveObjectUpdatesStatusAndResolvedAt() {
        ObjectRequest stored = new ObjectRequest(studentId, ObjectType.PROFILING, "motivo");
        when(repository.findById(any())).thenReturn(Optional.of(stored));
        when(repository.save(any(ObjectRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ObjectRequest resolved = service.resolveObject(UUID.randomUUID(), ObjectStatus.ACCEPTED, "Aceptada");

        assertThat(resolved.getStatus()).isEqualTo(ObjectStatus.ACCEPTED);
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.getResolutionNote()).isEqualTo("Aceptada");
    }

    @Test
    void activeRequestStopsProcessing() {
        ObjectRequest pending = new ObjectRequest(studentId, ObjectType.DIRECT_MARKETING, "motivo");
        when(repository.findFirstByStudentIdAndStatusInOrderByRequestedAtDesc(
                studentId, List.of(ObjectStatus.PENDING, ObjectStatus.ACCEPTED)))
                .thenReturn(Optional.of(pending));

        assertThat(service.isObjectActive(studentId)).isTrue();
        var status = service.getStatus(studentId);
        assertThat(status.objectActive()).isTrue();
        assertThat(status.objectType()).isEqualTo("DIRECT_MARKETING");
    }

    @Test
    void noRequestMeansInactive() {
        when(repository.findFirstByStudentIdAndStatusInOrderByRequestedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(repository.findFirstByStudentIdOrderByRequestedAtDesc(studentId)).thenReturn(Optional.empty());

        assertThat(service.isObjectActive(studentId)).isFalse();
        assertThat(service.getStatus(studentId).objectActive()).isFalse();
    }
}
