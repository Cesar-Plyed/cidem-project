package edu.cidemags.integrity_core.legal.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.domain.entity.AuditLog;
import edu.cidemags.integrity_core.legal.domain.model.ActorRole;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Escritura y consulta del audit trail inmutable (spec 39). */
@Service
public class AuditService {

    private static final int MAX_LIMIT = 100;

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditLog record(UUID actorId, ActorRole actorRole, String action, String entityType,
                           UUID entityId, Map<String, Object> previousState,
                           Map<String, Object> currentState, String ipAddress, String userAgent) {
        return repository.save(new AuditLog(actorId, actorRole, action, entityType, entityId,
                previousState, currentState, ipAddress, userAgent));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> query(UUID actorId, Instant beforeDate, Instant afterDate, Integer limit) {
        int size = limit == null ? MAX_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
        return repository.search(actorId, afterDate, beforeDate, PageRequest.of(0, size));
    }
}
