package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.Alert;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.sentinel.domain.ExamSessionId;
import edu.cidemags.integrity_core.sentinel.ports.out.AlertRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAlertRepositoryAdapter implements AlertRepositoryPort {

    /** Cota superior abierta para el filtro "hasta" cuando no se envia fecha. */
    private static final Instant MAX_INSTANT = Instant.parse("9999-12-31T23:59:59Z");

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

    @Override
    public List<Alert> findByExamId(UUID examId, String severity, Instant from, Instant to) {
        AlertSeverity parsedSeverity = parseSeverity(severity);
        // Sin filtro se envian todas las severidades y los extremos abiertos,
        // para no comparar parametros nulos en JPQL (PostgreSQL no infiere tipo).
        Collection<AlertSeverity> severities = parsedSeverity == null
            ? List.of(AlertSeverity.values())
            : List.of(parsedSeverity);
        Instant safeFrom = from == null ? Instant.EPOCH : from;
        Instant safeTo = to == null ? MAX_INSTANT : to;

        return jpaRepository.findForExam(examId, severities, safeFrom, safeTo).stream()
            .map(AlertMapper::toDomain)
            .toList();
    }

    @Override
    public List<Alert> findByAttemptId(UUID attemptId, int limit) {
        int safeLimit = Math.max(1, limit);
        return jpaRepository
            .findByAttemptIdOrderByGeneratedAtDesc(attemptId, PageRequest.of(0, safeLimit)).stream()
            .map(AlertMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Alert> findById(UUID id) {
        return jpaRepository.findById(id).map(AlertMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean markResolved(UUID id) {
        return jpaRepository.findById(id)
            .map(entity -> {
                entity.markResolved();
                jpaRepository.save(entity);
                return true;
            })
            .orElse(false);
    }

    private static AlertSeverity parseSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return null;
        }
        try {
            return AlertSeverity.valueOf(severity.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Severidad invalida: " + severity, ex);
        }
    }
}
