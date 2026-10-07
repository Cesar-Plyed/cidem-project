package edu.cidemags.integrity_core.oversight.adapters.out.persistence;

import edu.cidemags.integrity_core.oversight.domain.IntegritySnapshot;
import edu.cidemags.integrity_core.oversight.ports.out.IntegritySnapshotRepositoryPort;
import edu.cidemags.integrity_core.sentinel.domain.AttemptId;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaIntegritySnapshotRepositoryAdapter implements IntegritySnapshotRepositoryPort {

    private final IntegritySnapshotJpaRepository jpaRepository;

    public JpaIntegritySnapshotRepositoryAdapter(IntegritySnapshotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(IntegritySnapshot snapshot) {
        jpaRepository.save(IntegritySnapshotMapper.toEntity(snapshot));
    }

    @Override
    public Optional<IntegritySnapshot> findByAttemptId(AttemptId attemptId) {
        return jpaRepository.findByAttemptId(attemptId.value())
            .map(IntegritySnapshotMapper::toDomain);
    }

    @Override
    public List<IntegritySnapshot> findByExamSessionId(ExamSessionId examSessionId) {
        return jpaRepository.findByExamSessionId(examSessionId.value()).stream()
            .map(IntegritySnapshotMapper::toDomain)
            .toList();
    }
}