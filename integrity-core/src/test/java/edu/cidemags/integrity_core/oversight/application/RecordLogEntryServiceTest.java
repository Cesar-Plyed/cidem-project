package edu.cidemags.integrity_core.oversight.application;

import edu.cidemags.integrity_core.oversight.domain.*;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryCommand;
import edu.cidemags.integrity_core.oversight.ports.out.IntegritySnapshotRepositoryPort;
import edu.cidemags.integrity_core.oversight.ports.out.LogEntryRepositoryPort;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecordLogEntryServiceTest {

    private LogEntryRepositoryPort logEntryRepository;
    private IntegritySnapshotRepositoryPort snapshotRepository;
    private IntegrityScoringPolicy scoringPolicy;
    private RecordLogEntryService service;

    private final UUID examSessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        logEntryRepository = mock(LogEntryRepositoryPort.class);
        snapshotRepository = mock(IntegritySnapshotRepositoryPort.class);
        scoringPolicy = new IntegrityScoringPolicy();

        service = new RecordLogEntryService(
                logEntryRepository, snapshotRepository, scoringPolicy, mock(ApplicationEventPublisher.class));
    }

    private RecordLogEntryCommand command(AlertSeverity severity) {
        return new RecordLogEntryCommand(
            examSessionId, studentId, attemptId,
            LogSource.SENTINEL, severity, "Evento de prueba", Instant.now()
        );
    }

    @Test
    void shouldInitializeSnapshotWhenNoneExistsYet() {
        when(snapshotRepository.findByAttemptIdForUpdate(any())).thenReturn(Optional.empty());

        IntegritySnapshot result = service.record(command(AlertSeverity.MEDIUM));

        assertThat(result.score()).isEqualTo(90);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void shouldApplyPenaltyToExistingSnapshot() {
        IntegritySnapshot existing = new IntegritySnapshot(
            UUID.randomUUID(), new ExamSessionId(examSessionId), new StudentId(studentId), new AttemptId(attemptId),
            70, RiskLevel.MEDIUM, Instant.now()
        );
        when(snapshotRepository.findByAttemptIdForUpdate(any())).thenReturn(Optional.of(existing));

        IntegritySnapshot result = service.record(command(AlertSeverity.HIGH));

        assertThat(result.score()).isEqualTo(50);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void shouldSaveLogEntryWithCorrectFields() {
        when(logEntryRepository.existsByDedupKey(any())).thenReturn(false);
        when(snapshotRepository.findByAttemptIdForUpdate(any())).thenReturn(Optional.empty());

        service.record(command(AlertSeverity.LOW));

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logEntryRepository).save(captor.capture(), any());

        LogEntry saved = captor.getValue();
        assertThat(saved.examSessionId().value()).isEqualTo(examSessionId);
        assertThat(saved.studentId().value()).isEqualTo(studentId);
        assertThat(saved.attemptId().value()).isEqualTo(attemptId);
        assertThat(saved.source()).isEqualTo(LogSource.SENTINEL);
        assertThat(saved.severity()).isEqualTo(AlertSeverity.LOW);
    }

    @Test
    void shouldSkipPenaltyWhenSameEventArrivesTwice() {
        // El mismo hecho lógico (attempt+source+severity+descripción+segundo) ya
        // fue registrado (SafeGuard REST): la alerta duplicada de Sentinel por WS
        // NO penaliza de nuevo el índice.
        String key = "huella-repetida";
        when(logEntryRepository.existsByDedupKey(any())).thenReturn(true);
        IntegritySnapshot existing = new IntegritySnapshot(
            UUID.randomUUID(), new ExamSessionId(examSessionId), new StudentId(studentId), new AttemptId(attemptId),
            85, RiskLevel.LOW, Instant.now()
        );
        when(snapshotRepository.findByAttemptIdForUpdate(any())).thenReturn(Optional.of(existing));

        IntegritySnapshot result = service.record(command(AlertSeverity.LOW));

        verify(logEntryRepository, never()).save(any(LogEntry.class), any());
        verify(snapshotRepository, never()).save(any());
        assertThat(result.score()).isEqualTo(85); // sin penalización adicional
    }

    @Test
    void shouldSaveUpdatedSnapshot() {
        when(snapshotRepository.findByAttemptIdForUpdate(any())).thenReturn(Optional.empty());

        IntegritySnapshot result = service.record(command(AlertSeverity.LOW));

        verify(snapshotRepository).save(result);
    }
}