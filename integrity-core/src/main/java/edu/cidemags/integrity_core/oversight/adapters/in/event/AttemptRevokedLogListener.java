package edu.cidemags.integrity_core.oversight.adapters.in.event;

import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryCommand;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryUseCase;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.shared.events.ExamAttemptRevokedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AttemptRevokedLogListener {

    private final RecordLogEntryUseCase recordLogEntryUseCase;

    public AttemptRevokedLogListener(RecordLogEntryUseCase recordLogEntryUseCase) {
        this.recordLogEntryUseCase = recordLogEntryUseCase;
    }

    @EventListener
    public void onAttemptRevoked(ExamAttemptRevokedEvent event) {
        recordLogEntryUseCase.record(new RecordLogEntryCommand(
                event.examId(), event.studentId(), event.attemptId(), LogSource.EVALUATOR,
                AlertSeverity.HIGH, "Intento revocado por integridad académica", event.occurredAt()));
    }
}
