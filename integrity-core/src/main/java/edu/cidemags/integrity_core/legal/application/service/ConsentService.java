package edu.cidemags.integrity_core.legal.application.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import edu.cidemags.integrity_core.legal.application.dto.LegalViews;
import edu.cidemags.integrity_core.legal.domain.entity.ConsentLog;
import edu.cidemags.integrity_core.legal.domain.model.ConsentType;
import edu.cidemags.integrity_core.legal.infrastructure.persistence.ConsentLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion del consentimiento (specs 36, 40 y 48).
 *
 * <p>El historial es append-only: cada grant/revoke inserta una fila y el
 * estado vigente se deriva del ultimo registro. La revocacion solo afecta a
 * datos futuros; lo ya procesado se conserva segun la politica de retencion.</p>
 */
@Service
public class ConsentService {

    private final ConsentLogRepository repository;

    public ConsentService(ConsentLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ConsentLog grantConsent(UUID studentId, ConsentType consentType) {
        return grantConsent(studentId, consentType, null, null);
    }

    @Transactional
    public ConsentLog grantConsent(UUID studentId, ConsentType consentType, String ip, String userAgent) {
        return repository.save(ConsentLog.grant(studentId, consentType, ip, userAgent));
    }

    @Transactional
    public ConsentLog revokeConsent(UUID studentId, ConsentType consentType) {
        return revokeConsent(studentId, consentType, null, null);
    }

    @Transactional
    public ConsentLog revokeConsent(UUID studentId, ConsentType consentType, String ip, String userAgent) {
        return repository.save(ConsentLog.revoke(studentId, consentType, ip, userAgent));
    }

    /**
     * Revoca todos los consentimientos que esten vigentes. Los que ya estaban
     * revocados no generan ruido adicional en el historial.
     */
    @Transactional
    public List<ConsentLog> revokeAll(UUID studentId, String ip, String userAgent) {
        List<ConsentLog> revoked = new ArrayList<>();
        for (ConsentType type : ConsentType.values()) {
            if (isGranted(studentId, type)) {
                revoked.add(revokeConsent(studentId, type, ip, userAgent));
            }
        }
        return revoked;
    }

    @Transactional(readOnly = true)
    public boolean isGranted(UUID studentId, ConsentType consentType) {
        return latest(studentId, consentType).map(ConsentLog::isGranted).orElse(false);
    }

    /** true si el alumno tiene los cuatro consentimientos activos. */
    @Transactional(readOnly = true)
    public boolean hasAllConsents(UUID studentId) {
        for (ConsentType type : ConsentType.values()) {
            if (!isGranted(studentId, type)) {
                return false;
            }
        }
        return true;
    }

    @Transactional(readOnly = true)
    public LegalViews.ConsentOverview getStatus(UUID studentId) {
        List<LegalViews.ConsentStatus> statuses = new ArrayList<>();
        for (ConsentType type : ConsentType.values()) {
            Optional<ConsentLog> latest = latest(studentId, type);
            statuses.add(new LegalViews.ConsentStatus(
                    latest.map(ConsentLog::getId).orElse(null),
                    type.name(),
                    latest.map(log -> log.isGranted() ? "granted" : "revoked").orElse("revoked"),
                    latest.map(ConsentLog::getGrantedAt).orElse(null),
                    latest.map(ConsentLog::getRevokedAt).orElse(null)));
        }
        return new LegalViews.ConsentOverview(studentId, statuses);
    }

    @Transactional(readOnly = true)
    public LegalViews.ConsentHistory getHistory(UUID studentId) {
        List<LegalViews.ConsentHistoryEntry> history = repository
                .findByStudentIdOrderByCreatedAtAsc(studentId)
                .stream()
                .map(log -> new LegalViews.ConsentHistoryEntry(
                        log.getConsentType().name(),
                        log.isGranted() ? "granted" : "revoked",
                        log.effectiveAt()))
                .toList();
        return new LegalViews.ConsentHistory(studentId, history);
    }

    /** Estado vigente por tipo, util para integraciones internas. */
    @Transactional(readOnly = true)
    public Map<ConsentType, Boolean> statusMap(UUID studentId) {
        Map<ConsentType, Boolean> result = new EnumMap<>(ConsentType.class);
        for (ConsentType type : ConsentType.values()) {
            result.put(type, isGranted(studentId, type));
        }
        return result;
    }

    private Optional<ConsentLog> latest(UUID studentId, ConsentType consentType) {
        return repository.findTopByStudentIdAndConsentTypeOrderByCreatedAtDesc(studentId, consentType);
    }
}
