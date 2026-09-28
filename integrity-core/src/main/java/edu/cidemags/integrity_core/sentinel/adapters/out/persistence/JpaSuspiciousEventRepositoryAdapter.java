package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.domain.SuspiciousEvent;
import edu.cidemags.integrity_core.sentinel.ports.out.SuspiciousEventRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaSuspiciousEventRepositoryAdapter implements SuspiciousEventRepositoryPort {

    private final SuspiciousEventJpaRepository jpaRepository;

    public JpaSuspiciousEventRepositoryAdapter(SuspiciousEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(SuspiciousEvent event) {
        jpaRepository.save(SuspiciousEventMapper.toEntity(event));
    }

    @Override
    public List<SuspiciousEvent> findByExamSessionId(ExamSessionId examSessionId) {
        return jpaRepository.findByExamSessionId(examSessionId.value()).stream()
            .map(SuspiciousEventMapper::toDomain)
            .toList();
    }
}