package edu.cidemags.integrity_core.safeguard.application.listener;

import java.time.Instant;

import edu.cidemags.integrity_core.safeguard.domain.model.SafeguardSessionStatus;
import edu.cidemags.integrity_core.safeguard.domain.port.SafeguardSessionRepository;
import edu.cidemags.integrity_core.shared.events.ExamAttemptRevokedEvent;
import edu.cidemags.integrity_core.shared.events.IntegrityThresholdBreachedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TerminateSessionOnIntegrityThresholdListener {

    private final SafeguardSessionRepository sessionRepository;

    public TerminateSessionOnIntegrityThresholdListener(SafeguardSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @EventListener
    public void onIntegrityThresholdBreached(IntegrityThresholdBreachedEvent event) {
        terminate(event.attemptId());
    }

    @EventListener
    public void onAttemptRevoked(ExamAttemptRevokedEvent event) {
        terminate(event.attemptId());
    }

    private void terminate(java.util.UUID attemptId) {
        sessionRepository.findByAttemptId(attemptId).ifPresent(session -> {
            var locked = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();
            if (locked.getEndedAt() == null) {
                locked.setStatus(SafeguardSessionStatus.TERMINATED);
                locked.setEndedAt(Instant.now());
                sessionRepository.saveAndFlush(locked);
            }
        });
    }
}
