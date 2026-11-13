package edu.cidemags.integrity_core.legal.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.domain.entity.ObjectRequest;
import edu.cidemags.integrity_core.legal.domain.model.ObjectStatus;
import edu.cidemags.integrity_core.legal.domain.model.ObjectType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.ObjectRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derecho de objecion (Art. 21 GDPR, spec 46). */
@Service
public class ObjectService {

    private final ObjectRequestRepository repository;

    public ObjectService(ObjectRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ObjectRequest submitObject(UUID studentId, ObjectType objectType, String reason) {
        return repository.save(new ObjectRequest(studentId, objectType, reason));
    }

    @Transactional
    public ObjectRequest resolveObject(UUID objectId, ObjectStatus status, String resolutionNote) {
        ObjectRequest request = repository.findById(objectId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de objecion no encontrada: " + objectId));
        request.resolve(status, resolutionNote);
        return repository.save(request);
    }

    @Transactional(readOnly = true)
    public boolean isObjectActive(UUID studentId) {
        return activeRequest(studentId).isPresent();
    }

    @Transactional(readOnly = true)
    public LegalViews.ObjectStatusView getStatus(UUID studentId) {
        Optional<ObjectRequest> active = activeRequest(studentId);
        ObjectRequest request = active.orElseGet(() -> repository
                .findFirstByStudentIdOrderByRequestedAtDesc(studentId).orElse(null));
        if (request == null) {
            return new LegalViews.ObjectStatusView(null, false, null, null, null, null);
        }
        return new LegalViews.ObjectStatusView(request.getId(), active.isPresent(),
                request.getObjectType().name(), request.getReason(), request.getRequestedAt(),
                request.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<ObjectRequest> history(UUID studentId) {
        return repository.findByStudentIdOrderByRequestedAtDesc(studentId);
    }

    private Optional<ObjectRequest> activeRequest(UUID studentId) {
        return repository.findFirstByStudentIdAndStatusInOrderByRequestedAtDesc(
                studentId, List.of(ObjectStatus.PENDING, ObjectStatus.ACCEPTED));
    }
}
