package edu.cidemags.integrity_core.safeguard.application.listener;

import edu.cidemags.integrity_core.safeguard.domain.port.ExamAttemptPort;
import edu.cidemags.integrity_core.shared.events.IntegrityThresholdBreachedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FailAttemptOnIntegrityThresholdListener {

    private static final int MINIMUM_INTEGRITY_INDEX = 50;

    private final ExamAttemptPort examAttemptPort;

    public FailAttemptOnIntegrityThresholdListener(ExamAttemptPort examAttemptPort) {
        this.examAttemptPort = examAttemptPort;
    }

    @EventListener
    public void onIntegrityThresholdBreached(IntegrityThresholdBreachedEvent event) {
        if (event.integrityIndex() < MINIMUM_INTEGRITY_INDEX) {
            examAttemptPort.failAttemptForIntegrity(event.attemptId());
        }
    }
}
