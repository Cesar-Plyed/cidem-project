package edu.cidemags.integrity_core.legal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.ConsentLog;
import edu.cidemags.integrity_core.legal.domain.model.ConsentType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.ConsentLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private ConsentLogRepository repository;

    @InjectMocks
    private ConsentService service;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void grantInsertsConsentLogWithGrantedAt() {
        when(repository.save(any(ConsentLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.grantConsent(studentId, ConsentType.CAMERA, "127.0.0.1", "JUnit");

        ArgumentCaptor<ConsentLog> captor = ArgumentCaptor.forClass(ConsentLog.class);
        verify(repository).save(captor.capture());
        ConsentLog saved = captor.getValue();
        assertThat(saved.getStudentId()).isEqualTo(studentId);
        assertThat(saved.getConsentType()).isEqualTo(ConsentType.CAMERA);
        assertThat(saved.getGrantedAt()).isNotNull();
        assertThat(saved.getRevokedAt()).isNull();
        assertThat(saved.isGranted()).isTrue();
        assertThat(saved.getIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void revokeInsertsConsentLogWithRevokedAt() {
        when(repository.save(any(ConsentLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.revokeConsent(studentId, ConsentType.MICROPHONE);

        ArgumentCaptor<ConsentLog> captor = ArgumentCaptor.forClass(ConsentLog.class);
        verify(repository).save(captor.capture());
        ConsentLog saved = captor.getValue();
        assertThat(saved.getRevokedAt()).isNotNull();
        assertThat(saved.getGrantedAt()).isNull();
        assertThat(saved.isGranted()).isFalse();
    }

    @Test
    void isGrantedUsesLatestLog() {
        when(repository.findTopByStudentIdAndConsentTypeOrderByCreatedAtDesc(studentId, ConsentType.CAMERA))
                .thenReturn(Optional.of(ConsentLog.grant(studentId, ConsentType.CAMERA, null, null)));
        assertThat(service.isGranted(studentId, ConsentType.CAMERA)).isTrue();

        when(repository.findTopByStudentIdAndConsentTypeOrderByCreatedAtDesc(studentId, ConsentType.CAMERA))
                .thenReturn(Optional.of(ConsentLog.revoke(studentId, ConsentType.CAMERA, null, null)));
        assertThat(service.isGranted(studentId, ConsentType.CAMERA)).isFalse();
    }

    @Test
    void isGrantedIsFalseWithoutHistory() {
        when(repository.findTopByStudentIdAndConsentTypeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        assertThat(service.isGranted(studentId, ConsentType.SCREENSHOT)).isFalse();
    }

    @Test
    void revokeAllRevokesEveryGrantedType() {
        for (ConsentType type : ConsentType.values()) {
            when(repository.findTopByStudentIdAndConsentTypeOrderByCreatedAtDesc(studentId, type))
                    .thenReturn(Optional.of(ConsentLog.grant(studentId, type, null, null)));
        }
        when(repository.save(any(ConsentLog.class))).thenAnswer(inv -> inv.getArgument(0));

        var revoked = service.revokeAll(studentId, "10.0.0.1", "JUnit");

        assertThat(revoked).hasSize(ConsentType.values().length);
        assertThat(revoked).allMatch(log -> !log.isGranted());
    }

    @Test
    void historyIsAppendOnlyAndMapsLatestState() {
        ConsentLog granted = ConsentLog.grant(studentId, ConsentType.CAMERA, null, null);
        ConsentLog revoked = ConsentLog.revoke(studentId, ConsentType.CAMERA, null, null);
        when(repository.findByStudentIdOrderByCreatedAtAsc(studentId)).thenReturn(java.util.List.of(granted, revoked));

        var history = service.getHistory(studentId);

        assertThat(history.history()).hasSize(2);
        assertThat(history.history()).extracting("action").containsExactly("granted", "revoked");
    }

    @Test
    void statusExposesAllConsentTypes() {
        when(repository.findTopByStudentIdAndConsentTypeOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        var overview = service.getStatus(studentId);

        assertThat(overview.consents()).hasSize(ConsentType.values().length);
        assertThat(overview.consents()).allMatch(c -> "revoked".equals(c.status()));
    }
}
