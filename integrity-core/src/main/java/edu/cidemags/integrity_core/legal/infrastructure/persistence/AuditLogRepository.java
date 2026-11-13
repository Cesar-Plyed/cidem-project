package edu.cidemags.integrity_core.legal.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByActorIdOrderByOccurredAtAsc(UUID actorId);

    @Query("""
            select a from AuditLog a
            where (:actorId is null or a.actorId = :actorId)
              and (:afterDate is null or a.occurredAt >= :afterDate)
              and (:beforeDate is null or a.occurredAt <= :beforeDate)
            order by a.occurredAt desc
            """)
    List<AuditLog> search(@Param("actorId") UUID actorId,
                          @Param("afterDate") Instant afterDate,
                          @Param("beforeDate") Instant beforeDate,
                          Pageable pageable);
}
