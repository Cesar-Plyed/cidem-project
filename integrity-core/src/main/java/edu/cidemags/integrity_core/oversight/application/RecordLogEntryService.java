package edu.cidemags.integrity_core.oversight.application;

import edu.cidemags.integrity_core.oversight.domain.IntegrityScoringPolicy;
import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;
import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryCommand;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryUseCase;
import edu.cidemags.integrity_core.oversight.ports.out.IntegritySnapshotRepositoryPort;
import edu.cidemags.integrity_core.oversight.ports.out.LogEntryRepositoryPort;
import edu.cidemags.integrity_core.shared.events.IntegrityThresholdBreachedEvent;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RecordLogEntryService implements RecordLogEntryUseCase {

    private final LogEntryRepositoryPort logEntryRepository;
    private final IntegritySnapshotRepositoryPort snapshotRepository;
    private final IntegrityScoringPolicy scoringPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public RecordLogEntryService(
        LogEntryRepositoryPort logEntryRepository,
        IntegritySnapshotRepositoryPort snapshotRepository,
        IntegrityScoringPolicy scoringPolicy,
        ApplicationEventPublisher eventPublisher
    ) {
        this.logEntryRepository = logEntryRepository;
        this.snapshotRepository = snapshotRepository;
        this.scoringPolicy = scoringPolicy;
        this.eventPublisher = eventPublisher;
    }

    // Una sola transacción para log + snapshot: sin ella, un fallo entre los
    // dos saves deja la línea de tiempo sin su actualización de puntaje.
    @Override
    @Transactional
    public IntegritySnapshot record(RecordLogEntryCommand command) {
        ExamSessionId examSessionId = new ExamSessionId(command.examSessionId());
        StudentId studentId = new StudentId(command.studentId());
        AttemptId attemptId = new AttemptId(command.attemptId());

        String dedupKey = dedupKeyOf(command); // null si no viene el eventId en el payload
        if (dedupKey != null && logEntryRepository.existsByDedupKey(dedupKey)) {
            // Duplicado entre canales: SafeGuard ya registró esta violación vía REST
            // y Sentinel mandó la MISMA por WS. Solo penaliza una vez: retorna el
            // snapshot actual intacto.
            return snapshotRepository.findByAttemptIdForUpdate(attemptId)
                .orElseGet(() -> scoringPolicy.initialize(examSessionId, studentId, attemptId));
        }

        LogEntry logEntry = new LogEntry(
            UUID.randomUUID(), examSessionId, studentId, attemptId,
            command.source(), command.severity(), command.description(),
            command.occurredAt(), Instant.now()
        );
        logEntryRepository.save(logEntry, dedupKey);

        // Lock pesimista + transacción: serializa el read-modify-write del score
        // por intento; sin esto, dos eventos concurrentes se pisaban entre sí.
        IntegritySnapshot currentSnapshot = snapshotRepository.findByAttemptIdForUpdate(attemptId)
            .orElseGet(() -> scoringPolicy.initialize(examSessionId, studentId, attemptId));

        IntegritySnapshot updatedSnapshot = scoringPolicy.applyLogEntry(currentSnapshot, logEntry);
        snapshotRepository.save(updatedSnapshot);
        if (currentSnapshot.score() >= 50 && updatedSnapshot.score() < 50) {
            eventPublisher.publishEvent(new IntegrityThresholdBreachedEvent(
                    command.examSessionId(), command.attemptId(), command.studentId(),
                    updatedSnapshot.score(), command.occurredAt()));
        }

        return updatedSnapshot;
    }

    /**
     * Huella del "hecho lógico": intento + severidad + descripción + segundo de
     * ocurrencia. La descripción es la que ambos canales generan de forma
     * determinista para la misma violación (p. ej. "Cambio de aplicacion/pestana
     * detectado"), así que aunque el evento llegue por SafeGuard REST y por la
     * alerta de Sentinel por WS en el mismo segundo, la huella es la misma y solo
     * penaliza una vez. Si ambos canales traen eventId (single source), bastaría
     * usarlo; hoy solo SafeGuard lo lleva.
     */
    private static String dedupKeyOf(RecordLogEntryCommand command) {
        String second = command.occurredAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        String raw = command.attemptId() + "|" + command.source() + "|" + command.severity()
                + "|" + command.description() + "|" + second;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}