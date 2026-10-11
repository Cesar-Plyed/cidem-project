package edu.cidemags.integrity_core.oversight.adapters.in.event;

import edu.cidemags.integrity_core.oversight.domain.LogSource;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryCommand;
import edu.cidemags.integrity_core.oversight.ports.in.RecordLogEntryUseCase;
import edu.cidemags.integrity_core.sentinel.domain.AlertSeverity;
import edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Convierte cada bloqueo/riesgo de SafeGuard en una entrada de la línea de
 * tiempo de oversight (source = SAFEGUARD), de modo que el profesor vea las
 * alertas del examen en su panel sin tener que consultar la base de datos.
 * <p>
 * La severidad viene del {@code SafeguardRiskPolicy} de SafeGuard (1..5) y se
 * traduce a la escala LOW/MEDIUM/HIGH que usa oversight para descontar el
 * índice de integridad. El examSessionId es el id del examen: así los
 * endpoints /api/oversight/exams/{examId}/timeline y /summary los encuentran.
 */
@Component
public class SafeguardBlockEventListener {

    private final RecordLogEntryUseCase recordLogEntryUseCase;

    public SafeguardBlockEventListener(RecordLogEntryUseCase recordLogEntryUseCase) {
        this.recordLogEntryUseCase = recordLogEntryUseCase;
    }

    @EventListener
    public void onSafeguardBlock(SafeguardBlockEvent event) {
        if (event == null || event.examId() == null || event.attemptId() == null || event.studentId() == null) {
            return;
        }
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();
        recordLogEntryUseCase.record(new RecordLogEntryCommand(
                event.examId(),
                event.studentId(),
                event.attemptId(),
                LogSource.SAFEGUARD,
                toSeverity(payload.get("severity")),
                describe(payload.get("eventType")),
                event.occurredAt()));
    }

    /** Severidad de riesgo de SafeGuard (1 leve .. 5 critico) a la escala de alertas de oversight. */
    private static AlertSeverity toSeverity(Object rawSeverity) {
        int severity = rawSeverity instanceof Number number ? number.intValue() : 1;
        if (severity >= 3) {
            return AlertSeverity.HIGH;
        }
        if (severity == 2) {
            return AlertSeverity.MEDIUM;
        }
        return AlertSeverity.LOW;
    }

    /** Mensaje legible para la línea de tiempo del profesor. */
    private static String describe(Object rawType) {
        String type = rawType == null ? "" : rawType.toString();
        return switch (type) {
            case "TAB_SWITCH" -> "Cambio de aplicacion/pestana detectado";
            case "WINDOW_BLUR" -> "Perdida del foco de la ventana del examen";
            case "FULLSCREEN_EXIT" -> "La ventana del examen no esta en pantalla completa";
            case "COPY_PASTE" -> "Intento de copiar/pegar bloqueado";
            case "RIGHT_CLICK" -> "Clic derecho bloqueado";
            case "NETWORK_DISCONNECT" -> "Conexion interrumpida durante el examen";
            case "SUSPICIOUS_ACTIVITY" -> "Actividad sospechosa durante el examen";
            default -> "Evento de SafeGuard: " + (type.isBlank() ? "desconocido" : type);
        };
    }
}
