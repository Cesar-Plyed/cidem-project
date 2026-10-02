package edu.cidemags.integrity_core.oversight.application;

import edu.cidemags.integrity_core.oversight.domain.IntegrityScoringPolicy;
import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;
import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryCommand;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryUseCase;
import edu.cidemags.integrity_core.oversight.ports.out.IntegritySnapshotRepositoryPort;
import edu.cidemags.integrity_core.oversight.ports.out.LogEntryRepositoryPort;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.StudentId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RecordLogEntryService implements RecordLogEntryUseCase {

    private final LogEntryRepositoryPort logEntryRepository;
    private final IntegritySnapshotRepositoryPort snapshotRepository;
    private final IntegrityScoringPolicy scoringPolicy;

    public RecordLogEntryService(
        LogEntryRepositoryPort logEntryRepository,
        IntegritySnapshotRepositoryPort snapshotRepository,
        IntegrityScoringPolicy scoringPolicy
    ) {
        this.logEntryRepository = logEntryRepository;
        this.snapshotRepository = snapshotRepository;
        this.scoringPolicy = scoringPolicy;
    }

    @Override
    public IntegritySnapshot record(RecordLogEntryCommand command) {
        ExamSessionId examSessionId = new ExamSessionId(command.examSessionId());
        StudentId studentId = new StudentId(command.studentId());
        AttemptId attemptId = new AttemptId(command.attemptId());

        LogEntry logEntry = new LogEntry(
            UUID.randomUUID(), examSessionId, studentId, attemptId,
            command.source(), command.severity(), command.description(),
            command.occurredAt(), Instant.now()
        );
        logEntryRepository.save(logEntry);

        IntegritySnapshot currentSnapshot = snapshotRepository.findByAttemptId(attemptId)
            .orElseGet(() -> scoringPolicy.initialize(examSessionId, studentId, attemptId));

        IntegritySnapshot updatedSnapshot = scoringPolicy.applyLogEntry(currentSnapshot, logEntry);
        snapshotRepository.save(updatedSnapshot);

        return updatedSnapshot;
    }
}