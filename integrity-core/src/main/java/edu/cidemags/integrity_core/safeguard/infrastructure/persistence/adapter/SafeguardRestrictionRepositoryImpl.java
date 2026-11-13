package edu.cidemags.integrity_core.safeguard.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import edu.cidemags.integrity_core.safeguard.domain.model.Restriction;
import edu.cidemags.integrity_core.safeguard.domain.port.RestrictionRepository;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.entity.SafeguardRestrictionEntity;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.mapper.SafeguardRestrictionMapper;
import edu.cidemags.integrity_core.safeguard.infrastructure.persistence.port.SafeguardRestrictionJpaRepository;

@Repository
public class SafeguardRestrictionRepositoryImpl implements RestrictionRepository {

    private final SafeguardRestrictionJpaRepository jpaRepository;
    private final SafeguardRestrictionMapper mapper;

    public SafeguardRestrictionRepositoryImpl(SafeguardRestrictionJpaRepository jpaRepository,
                                              SafeguardRestrictionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Restriction> findByExamId(UUID examId) {
        return jpaRepository.findByExamIdOrderByTypeAsc(examId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<Restriction> replaceForExam(UUID examId, List<Restriction> restrictions) {
        // Semántica PUT: la lista enviada sustituye por completo a la anterior.
        jpaRepository.deleteByExamId(examId);
        List<SafeguardRestrictionEntity> entities = restrictions.stream()
                .map(restriction -> mapper.toEntity(examId, restriction))
                .toList();
        jpaRepository.flush();
        return jpaRepository.saveAllAndFlush(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
