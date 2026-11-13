package edu.cidemags.integrity_core.legal.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.domain.entity.RestrictionRequest;
import edu.cidemags.integrity_core.legal.domain.model.RestrictionStatus;
import edu.cidemags.integrity_core.legal.domain.model.RestrictionType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.RestrictionRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Limitacion del tratamiento (Art. 18 GDPR, spec 47). */
@Service
public class RestrictionService {

    private static final Duration DEFAULT_DURATION = Duration.ofDays(365);

    private final RestrictionRequestRepository repository;

    public RestrictionService(RestrictionRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RestrictionRequest submitRestriction(UUID studentId, RestrictionType restrictionType, String reason) {
        return repository.save(new RestrictionRequest(studentId, restrictionType, reason,
                Instant.now().plus(DEFAULT_DURATION)));
    }

    @Transactional
    public RestrictionRequest revokeRestriction(UUID restrictionId) {
        RestrictionRequest request = repository.findById(restrictionId)
                .orElseThrow(() -> new IllegalArgumentException("Restriccion no encontrada: " + restrictionId));
        request.revoke();
        return repository.save(request);
    }

    @Transactional(readOnly = true)
    public RestrictionRequest get(UUID restrictionId) {
        return repository.findById(restrictionId)
                .orElseThrow(() -> new IllegalArgumentException("Restriccion no encontrada: " + restrictionId));
    }

    @Transactional(readOnly = true)
    public boolean isRestricted(UUID studentId) {
        return activeRestriction(studentId).isPresent();
    }

    @Transactional(readOnly = true)
    public LegalViews.RestrictionStatusView getStatus(UUID studentId) {
        Optional<RestrictionRequest> active = activeRestriction(studentId);
        RestrictionRequest request = active.orElseGet(() -> repository
                .findFirstByStudentIdAndStatusOrderByRequestedAtDesc(studentId, RestrictionStatus.REVOKED)
                .orElse(null));
        if (request == null) {
            return new LegalViews.RestrictionStatusView(null, false, null, null, null, null, null);
        }
        return new LegalViews.RestrictionStatusView(request.getId(), active.isPresent(),
                request.getRestrictionType().name(), request.getReason(), request.getRequestedAt(),
                request.getExpiresAt(), request.getStatus().name());
    }

    private Optional<RestrictionRequest> activeRestriction(UUID studentId) {
        return repository
                .findFirstByStudentIdAndStatusOrderByRequestedAtDesc(studentId, RestrictionStatus.ACTIVE)
                .filter(RestrictionRequest::isActive);
    }
}
