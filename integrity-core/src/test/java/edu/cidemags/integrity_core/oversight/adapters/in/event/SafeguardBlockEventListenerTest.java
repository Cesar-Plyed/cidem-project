package edu.cidemags.integrity_core.oversight.adapters.in.event;

import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryCommand;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryUseCase;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SafeguardBlockEventListenerTest {

    @Test
    void writesALogEntryWithSafeguardSourceAndHighSeverityForFullscreenExit() {
        RecordLogEntryUseCase recordLogEntry = mock(RecordLogEntryUseCase.class);
        SafeguardBlockEventListener listener = new SafeguardBlockEventListener(recordLogEntry);

        UUID examId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        listener.onSafeguardBlock(new SafeguardBlockEvent(
                UUID.randomUUID(), examId, attemptId, UUID.randomUUID(), studentId, occurredAt,
                SafeguardBlockEvent.SOURCE_SAFEGUARD,
                Map.of("eventType", "FULLSCREEN_EXIT", "severity", 3, "riskPoints", 10, "totalRiskScore", 10)));

        ArgumentCaptor<RecordLogEntryCommand> captor = ArgumentCaptor.forClass(RecordLogEntryCommand.class);
        verify(recordLogEntry).record(captor.capture());
        RecordLogEntryCommand command = captor.getValue();
        // examSessionId = examen: los endpoints /api/oversight/exams/{examId}/... buscan por ese valor.
        assertEquals(examId, command.examSessionId());
        assertEquals(attemptId, command.attemptId());
        assertEquals(studentId, command.studentId());
        assertEquals(LogSource.SAFEGUARD, command.source());
        assertEquals(AlertSeverity.HIGH, command.severity());
        assertTrue(command.description().toLowerCase().contains("pantalla completa"));
        assertEquals(occurredAt, command.occurredAt());
    }

    @Test
    void mapsLowAndMediumRiskSeverities() {
        RecordLogEntryUseCase recordLogEntry = mock(RecordLogEntryUseCase.class);
        SafeguardBlockEventListener listener = new SafeguardBlockEventListener(recordLogEntry);

        listener.onSafeguardBlock(new SafeguardBlockEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), SafeguardBlockEvent.SOURCE_SAFEGUARD,
                Map.of("eventType", "WINDOW_BLUR", "severity", 1)));
        listener.onSafeguardBlock(new SafeguardBlockEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), SafeguardBlockEvent.SOURCE_SAFEGUARD,
                Map.of("eventType", "TAB_SWITCH", "severity", 2)));

        ArgumentCaptor<RecordLogEntryCommand> captor = ArgumentCaptor.forClass(RecordLogEntryCommand.class);
        verify(recordLogEntry, org.mockito.Mockito.times(2)).record(captor.capture());
        assertEquals(AlertSeverity.LOW, captor.getAllValues().get(0).severity());
        assertEquals(AlertSeverity.MEDIUM, captor.getAllValues().get(1).severity());
    }
}
