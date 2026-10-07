package edu.cidemags.integrity_core.oversight.adapters.in.event;

import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryCommand;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryUseCase;
import edu.cidemags.integrity_core.sentinel.application.event.AlertGeneratedEvent;
import edu.cidemags.integrity_core.sentinel.domain.Alert;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AlertGeneratedEventListener {

    private final RecordLogEntryUseCase recordLogEntryUseCase;

    public AlertGeneratedEventListener(RecordLogEntryUseCase recordLogEntryUseCase) {
        this.recordLogEntryUseCase = recordLogEntryUseCase;
    }

    @EventListener
    public void onAlertGenerated(AlertGeneratedEvent event) {
        Alert alert = event.alert();

        RecordLogEntryCommand command = new RecordLogEntryCommand(
            alert.examSessionId().value(),
            alert.studentId().value(),
            alert.attemptId().value(),
            LogSource.SENTINEL,
            alert.severity(),
            alert.reason(),
            alert.generatedAt()
        );

        recordLogEntryUseCase.record(command);
    }
}