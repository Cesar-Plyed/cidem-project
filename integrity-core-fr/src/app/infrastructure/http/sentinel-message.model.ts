/**
 * Mensaje que el frontend envía a Sentinel por STOMP (/ws/sentinel → /app/sentinel/events).
 *
 * El backend NO confía en los ids del payload (los contrasta con el principal del JWT),
 * pero attemptId es obligatorio: lo necesita para asociar el evento al intento.
 */
export interface SentinelEventMessage {
  examSessionId: string;
  /** Se ignora en el backend (viene del JWT); se envía para trazabilidad. */
  studentId: string;
  attemptId: string;
  type: 'WINDOW_FOCUS_LOST' | 'WINDOW_FOCUS_REGAINED' | 'TAB_OR_APP_SWITCH';
  occurredAt: string;
}

/** Alerta que Sentinel devuelve por /user/queue/sentinel/alerts. */
export interface SentinelAlertNotification {
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  reason: string;
  generatedAt: string;
}
