package edu.cidemags.integrity_core.oversight.application;

import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;
import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.oversight.ports.in.GetAttemptIntegrityUseCase;
import edu.cidemags.integrity_core.oversight.ports.in.GetExamSummaryUseCase;
import edu.cidemags.integrity_core.oversight.ports.in.GetExamTimelineUseCase;
import edu.cidemags.integrity_core.oversight.ports.out.IntegritySnapshotRepositoryPort;
import edu.cidemags.integrity_core.oversight.ports.out.LogEntryRepositoryPort;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OversightQueryService implements GetExamSummaryUseCase, GetExamTimelineUseCase, GetAttemptIntegrityUseCase {

    private final IntegritySnapshotRepositoryPort snapshotRepository;
    private final LogEntryRepositoryPort logEntryRepository;

    public OversightQueryService(
        IntegritySnapshotRepositoryPort snapshotRepository,
        LogEntryRepositoryPort logEntryRepository
    ) {
        this.snapshotRepository = snapshotRepository;
        this.logEntryRepository = logEntryRepository;
    }

    @Override
    public List<IntegritySnapshot> getSummary(UUID examSessionId) {
        return snapshotRepository.findByExamSessionId(new ExamSessionId(examSessionId));
    }

    @Override
    public List<LogEntry> getTimeline(UUID examSessionId) {
        return logEntryRepository.findByExamSessionId(new ExamSessionId(examSessionId));
    }

    @Override
    public Optional<IntegritySnapshot> getIntegrity(UUID attemptId) {
        return snapshotRepository.findByAttemptId(new AttemptId(attemptId));
    }
}