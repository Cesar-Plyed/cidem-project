package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.ports.out.AlertRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaAlertRepositoryAdapter implements AlertRepositoryPort {

    private final AlertJpaRepository jpaRepository;

    public JpaAlertRepositoryAdapter(AlertJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Alert alert) {
        jpaRepository.save(AlertMapper.toEntity(alert));
    }

    @Override
    public List<Alert> findByExamSessionId(ExamSessionId examSessionId) {
        return jpaRepository.findByExamSessionId(examSessionId.value()).stream()
            .map(AlertMapper::toDomain)
            .toList();
    }
}