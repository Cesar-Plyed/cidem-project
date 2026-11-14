package edu.cidemags.integrity_core.sentinel.adapters.out.persistence;

import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AlertJpaRepository extends JpaRepository<AlertEntity, UUID> {

    List<AlertEntity> findByExamSessionId(UUID examSessionId);

    List<AlertEntity> findByAttemptIdOrderByGeneratedAtDesc(UUID attemptId, Pageable pageable);

    /**
     * Alertas de un examen con filtros ya resueltos por el adapter.
     *
     * <p>No se compara contra parametros nulos (PostgreSQL no puede inferir su
     * tipo): el adapter pasa la lista completa de severidades cuando no hay
     * filtro y los extremos de fecha siempre con valor.</p>
     */
    @Query("""
        SELECT a FROM AlertEntity a
        WHERE a.examId = :examId
          AND a.severity IN :severities
          AND a.generatedAt >= :from
          AND a.generatedAt <= :to
        ORDER BY a.generatedAt DESC
        """)
    List<AlertEntity> findForExam(
        @Param("examId") UUID examId,
        @Param("severities") Collection<AlertSeverity> severities,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
