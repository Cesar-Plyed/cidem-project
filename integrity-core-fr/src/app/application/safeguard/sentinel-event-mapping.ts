import { SafeguardEvent } from '../../domain/models/safeguard/safeguard.model';
import { SentinelEventMessage } from '../../infrastructure/http/sentinel-message.model';

export type SafeguardEventWithSessionId = SafeguardEvent & { sessionId: string };

export type SentinelEventType = SentinelEventMessage['type'];

/**
 * Traduce un tipo de evento del monitor web a los tres `EventType` que el
 * backend de Sentinel entiende. Los eventos que Sentinel no modela (copiar,
 * pegar, devtools, cámara…) no se envían y devuelven null.
 */
export function sentinelEventTypeOf(type: SafeguardEvent['type']): SentinelEventType | null {
  switch (type) {
    case 'TAB_HIDDEN':
      return 'TAB_OR_APP_SWITCH';
    case 'WINDOW_BLUR':
      return 'WINDOW_FOCUS_LOST';
    // WINDOW_FOCUS_REGAINED no se genera hoy: el monitor solo reporta violaciones.
    default:
      return null;
  }
}
