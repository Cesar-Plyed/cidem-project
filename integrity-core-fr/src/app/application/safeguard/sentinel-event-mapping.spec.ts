import { SafeguardEvent } from '../../domain/models/safeguard/safeguard.model';
import { sentinelEventTypeOf } from './sentinel-event-mapping';
import { mapEventType } from '../../infrastructure/http/safeguard-http.repository';

describe('sentinel-event-mapping', () => {
  it('mapea TAB_HIDDEN a TAB_OR_APP_SWITCH y WINDOW_BLUR a WINDOW_FOCUS_LOST', () => {
    expect(sentinelEventTypeOf('TAB_HIDDEN')).toBe('TAB_OR_APP_SWITCH');
    expect(sentinelEventTypeOf('WINDOW_BLUR')).toBe('WINDOW_FOCUS_LOST');
  });

  it('los eventos que Sentinel no modela no se envían', () => {
    expect(sentinelEventTypeOf('COPY_ATTEMPT')).toBeNull();
    expect(sentinelEventTypeOf('CUT_ATTEMPT')).toBeNull();
    expect(sentinelEventTypeOf('PASTE_ATTEMPT')).toBeNull();
    expect(sentinelEventTypeOf('FULLSCREEN_EXIT')).toBeNull();
    expect(sentinelEventTypeOf('CONTEXT_MENU')).toBeNull();
    expect(sentinelEventTypeOf('RESTRICTED_SHORTCUT')).toBeNull();
    expect(sentinelEventTypeOf('DEVTOOLS_OPENED')).toBeNull();
    expect(sentinelEventTypeOf('PRINT_ATTEMPT')).toBeNull();
    expect(sentinelEventTypeOf('NATIVE_WINDOW_SWITCH')).toBeNull();
  });
});

describe('mapEventType (adaptador HTTP de SafeGuard)', () => {
  function evt(type: SafeguardEvent['type']): SafeguardEvent {
    return {
      eventId: 'e1',
      examId: 'exam-1',
      attemptId: 'attempt-1',
      sessionId: 'session-1',
      type,
      result: 'BLOCKED',
      occurredAt: new Date().toISOString(),
    };
  }

  it('los tipos nuevos llegan al enum correcto del backend', () => {
    expect(mapEventType(evt('DEVTOOLS_OPENED').type)).toBe('DEVTOOLS_OPENED');
    // La impresión no tiene tipo propio en el enum de SafeGuard v1: es una
    // actividad sospechosa (el backend le asigna severidad 5).
    expect(mapEventType(evt('PRINT_ATTEMPT').type)).toBe('SUSPICIOUS_ACTIVITY');
    // El cambio de ventana nativa es un cambio de contexto ap, equivalente a TAB_SWITCH.
    expect(mapEventType(evt('NATIVE_WINDOW_SWITCH').type)).toBe('TAB_SWITCH');
  });
});
