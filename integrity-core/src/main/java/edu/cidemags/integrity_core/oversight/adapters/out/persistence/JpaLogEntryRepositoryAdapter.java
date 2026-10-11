package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import edu.cidemags.integrity_core.oversight.domain.LogEntry;
import edu.cidemags.integrity_core.oversight.ports.out.LogEntryRepositoryPort;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaLogEntryRepositoryAdapter implements LogEntryRepositoryPort {

    private final LogEntryJpaRepository jpaRepository;

    public JpaLogEntryRepositoryAdapter(LogEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(LogEntry entry) {
        jpaRepository.save(LogEntryMapper.toEntity(entry, null));
    }

    @Override
    public void save(LogEntry entry, String dedupKey) {
        jpaRepository.save(LogEntryMapper.toEntity(entry, dedupKey));
    }

    @Override
    public boolean existsByDedupKey(String dedupKey) {
        return jpaRepository.existsByDedupKey(dedupKey);
    }

    @Override
    public List<LogEntry> findByExamSessionId(ExamSessionId examSessionId) {
        return jpaRepository.findByExamSessionId(examSessionId.value()).stream()
            .map(LogEntryMapper::toDomain)
            .toList();
    }

    @Override
    public List<LogEntry> findByAttemptId(AttemptId attemptId) {
        return jpaRepository.findByAttemptId(attemptId.value()).stream()
            .map(LogEntryMapper::toDomain)
            .toList();
    }
}