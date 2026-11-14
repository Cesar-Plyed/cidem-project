import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import {
  DEFAULT_SAFEGUARD_CONFIG,
  SafeguardConfig,
  SafeguardEvent,
  SafeguardSession,
} from '../../domain/models/safeguard/safeguard.model';
import { SafeguardPlatform } from '../../domain/ports/safeguard-platform';
import { GetSafeguardConfigUseCase } from '../use-cases/safeguard/get-safeguard-config.use-case';
import { StartSafeguardSessionUseCase } from '../use-cases/safeguard/start-safeguard-session.use-case';
import { SendSafeguardHeartbeatUseCase } from '../use-cases/safeguard/send-safeguard-heartbeat.use-case';
import { CloseSafeguardSessionUseCase } from '../use-cases/safeguard/close-safeguard-session.use-case';
import { SafeguardEventDispatcher } from './safeguard-event-dispatcher';
import { SafeguardMonitorService } from './safeguard-monitor.service';
import { SentinelStompService } from '../../infrastructure/http/sentinel-stomp.service';

function session(id = 'session-1'): SafeguardSession {
  return { sessionId: id, examId: 'exam-1', attemptId: 'attempt-1', startedAt: new Date().toISOString() };
}

describe('SafeguardMonitorService', () => {
  let getConfig: ReturnType<typeof vi.fn<(examId: string) => Observable<SafeguardConfig>>>;
  let startSession: ReturnType<typeof vi.fn<() => Observable<SafeguardSession>>>;
  let heartbeat: ReturnType<typeof vi.fn<() => Observable<void>>>;
  let closeSession: ReturnType<typeof vi.fn<() => Observable<void>>>;
  let enqueue: ReturnType<typeof vi.fn<(event: SafeguardEvent) => void>>;
  let enterProtectedMode: ReturnType<typeof vi.fn<() => Promise<void>>>;
  let exitProtectedMode: ReturnType<typeof vi.fn<() => Promise<void>>>;
  let sentinelPublish: ReturnType<typeof vi.fn<(event: unknown) => Promise<void>>>;
  let sentinelDisconnect: ReturnType<typeof vi.fn<() => Promise<void>>>;
  let sentinelConnect: ReturnType<typeof vi.fn<() => Promise<void>>>;
  let monitor: SafeguardMonitorService;

  beforeEach(() => {
    vi.useFakeTimers();

    getConfig = vi.fn(() => of(DEFAULT_SAFEGUARD_CONFIG));
    startSession = vi.fn(() => of(session()));
    heartbeat = vi.fn(() => of(undefined));
    closeSession = vi.fn(() => of(undefined));
    enqueue = vi.fn();
    enterProtectedMode = vi.fn(() => Promise.resolve());
    exitProtectedMode = vi.fn(() => Promise.resolve());
    sentinelPublish = vi.fn(() => Promise.resolve());
    sentinelDisconnect = vi.fn(() => Promise.resolve());
    sentinelConnect = vi.fn(() => Promise.resolve());

    TestBed.configureTestingModule({
      providers: [
        { provide: GetSafeguardConfigUseCase, useValue: { execute: getConfig } },
        { provide: StartSafeguardSessionUseCase, useValue: { execute: startSession } },
        { provide: SendSafeguardHeartbeatUseCase, useValue: { execute: heartbeat } },
        { provide: CloseSafeguardSessionUseCase, useValue: { execute: closeSession } },
        {
          provide: SafeguardPlatform,
          useValue: {
            kind: 'web',
            capabilities: { nativeFocusTracking: false, kioskMode: false, osShortcutBlocking: false },
            enterProtectedMode,
            exitProtectedMode,
            onNativeViolation: () => () => undefined,
          },
        },
        {
          provide: SafeguardEventDispatcher,
          useValue: {
            enqueue,
            finalize: vi.fn(),
            resume: vi.fn(),
            configure: vi.fn(),
            flushNow: vi.fn(),
            pending: () => 0,
            unreachable: () => false,
          },
        },
        {
          provide: SentinelStompService,
          useValue: {
            connectForExam: sentinelConnect,
            publish: sentinelPublish,
            disconnect: sentinelDisconnect,
          },
        },
      ],
    });

    monitor = TestBed.inject(SafeguardMonitorService);
  });

  afterEach(() => {
    monitor.stop();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  function start(): void {
    monitor.start('exam-1', 'attempt-1');
    vi.advanceTimersByTime(0); // resuelve las promesas/observables síncronos encolados
  }

  /** Finge el estado de pantalla completa del documento (Fullscreen API no existe en jsdom). */
  function defineFullscreenElement(element: Element | null): void {
    Object.defineProperty(document, 'fullscreenElement', { configurable: true, get: () => element });
  }

  it('al iniciar entra en modo protegido, pide la config y abre sesión', () => {
    start();
    expect(monitor.active()).toBe(true);
    expect(enterProtectedMode).toHaveBeenCalledTimes(1);
    expect(getConfig).toHaveBeenCalledWith('exam-1');
    expect(startSession).toHaveBeenCalledWith('exam-1', 'attempt-1');
  });

  it('si la config no carga, protege igual con la política estricta por defecto', () => {
    getConfig.mockReturnValue(throwError(() => new Error('network')));
    start();

    expect(monitor.active()).toBe(true);
    document.dispatchEvent(new Event('copy', { cancelable: true }));
    // sigue bloqueando copiar aunque el servidor nunca respondió
    expect(enqueue).toHaveBeenCalledWith(expect.objectContaining({ type: 'COPY_ATTEMPT', result: 'BLOCKED' }));
  });

  it('si el examen no tiene SafeGuard configurado (404), no protege ni abre sesión', () => {
    getConfig.mockReturnValue(throwError(() => ({ status: 404 })));
    start();

    expect(monitor.active()).toBe(false);
    expect(startSession).not.toHaveBeenCalled();
    expect(exitProtectedMode).toHaveBeenCalledTimes(1);

    const copyEvent = new Event('copy', { cancelable: true });
    document.dispatchEvent(copyEvent);
    expect(copyEvent.defaultPrevented).toBe(false);
  });

  it('si el servidor rechaza abrir la sesión de forma definitiva (403/404), deja de reintentar', () => {
    startSession.mockReturnValue(throwError(() => ({ status: 403 })));
    start();
    expect(startSession).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(DEFAULT_SAFEGUARD_CONFIG.heartbeatIntervalSeconds * 1000 * 3);
    expect(startSession).toHaveBeenCalledTimes(1);
    expect(monitor.active()).toBe(true); // la protección local sigue activa
  });

  it('si abrir la sesión falla por red o por el servidor (5xx), reintenta en el siguiente latido', () => {
    startSession.mockReturnValueOnce(throwError(() => ({ status: 503 })));
    start();
    expect(startSession).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(DEFAULT_SAFEGUARD_CONFIG.heartbeatIntervalSeconds * 1000);
    expect(startSession).toHaveBeenCalledTimes(2);
  });

  it('si el servidor desactiva SafeGuard para el examen, el monitor se detiene solo', () => {
    getConfig.mockReturnValue(of({ ...DEFAULT_SAFEGUARD_CONFIG, enabled: false }));
    start();

    expect(monitor.active()).toBe(false);
    expect(exitProtectedMode).toHaveBeenCalledTimes(1);
  });

  it('bloquea copiar, cortar, pegar y el menú contextual, y los reporta', () => {
    start();

    const copyEvent = new Event('copy', { cancelable: true });
    document.dispatchEvent(copyEvent);
    expect(copyEvent.defaultPrevented).toBe(true);

    const menuEvent = new Event('contextmenu', { cancelable: true });
    document.dispatchEvent(menuEvent);
    expect(menuEvent.defaultPrevented).toBe(true);

    const types = enqueue.mock.calls.map((call) => call[0].type);
    expect(types).toContain('COPY_ATTEMPT');
    expect(types).toContain('CONTEXT_MENU');
  });

  it('respeta la configuración del servidor: si blockCopy es false, no bloquea copiar', () => {
    getConfig.mockReturnValue(of({ ...DEFAULT_SAFEGUARD_CONFIG, blockCopy: false }));
    start();

    const copyEvent = new Event('copy', { cancelable: true });
    document.dispatchEvent(copyEvent);
    expect(copyEvent.defaultPrevented).toBe(false);
    expect(enqueue).not.toHaveBeenCalledWith(expect.objectContaining({ type: 'COPY_ATTEMPT' }));
  });

  it('detecta el cambio de pestaña (visibilitychange)', () => {
    start();
    Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => 'hidden' });
    Object.defineProperty(document, 'hidden', { configurable: true, get: () => true });

    document.dispatchEvent(new Event('visibilitychange'));

    expect(enqueue).toHaveBeenCalledWith(expect.objectContaining({ type: 'TAB_HIDDEN', result: 'DETECTED' }));
  });

  it('bloquea un atajo restringido (F12) y lo reporta con su nombre', () => {
    start();
    const keyEvent = new KeyboardEvent('keydown', { key: 'F12', cancelable: true });
    document.dispatchEvent(keyEvent);

    expect(keyEvent.defaultPrevented).toBe(true);
    expect(enqueue).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'RESTRICTED_SHORTCUT', result: 'BLOCKED', detail: 'F12' }),
    );
  });

  it('Ctrl+C por teclado se reporta como intento de copiar, no como atajo genérico', () => {
    start();
    const keyEvent = new KeyboardEvent('keydown', { key: 'c', ctrlKey: true, cancelable: true });
    document.dispatchEvent(keyEvent);

    expect(keyEvent.defaultPrevented).toBe(true);
    expect(enqueue).toHaveBeenCalledWith(expect.objectContaining({ type: 'COPY_ATTEMPT', result: 'BLOCKED' }));
  });

  it('mantener una tecla presionada (repeat) no genera reportes repetidos', () => {
    start();
    const first = new KeyboardEvent('keydown', { key: 'F12', cancelable: true, repeat: false });
    const repeat = new KeyboardEvent('keydown', { key: 'F12', cancelable: true, repeat: true });
    document.dispatchEvent(first);
    document.dispatchEvent(repeat);
    document.dispatchEvent(repeat);

    const shortcuts = enqueue.mock.calls.filter((call) => call[0].type === 'RESTRICTED_SHORTCUT');
    expect(shortcuts).toHaveLength(1);
  });

  it('aplica throttling: dos eventos iguales seguidos solo reportan uno al backend', () => {
    start();
    document.dispatchEvent(new Event('copy', { cancelable: true }));
    document.dispatchEvent(new Event('copy', { cancelable: true }));

    const copyReports = enqueue.mock.calls.filter((call) => call[0].type === 'COPY_ATTEMPT');
    expect(copyReports).toHaveLength(1);
  });

  it('vuelve a reportar el mismo tipo de evento pasado el tiempo de throttling', () => {
    start();
    document.dispatchEvent(new Event('copy', { cancelable: true }));
    vi.advanceTimersByTime(DEFAULT_SAFEGUARD_CONFIG.eventThrottleMs + 1);
    document.dispatchEvent(new Event('copy', { cancelable: true }));

    const copyReports = enqueue.mock.calls.filter((call) => call[0].type === 'COPY_ATTEMPT');
    expect(copyReports).toHaveLength(2);
  });

  it('muestra un aviso visible en cada ocurrencia, aunque el reporte esté en throttling', () => {
    start();
    // Al arrancar sin pantalla completa ya salió el aviso FULLSCREEN_EXIT.
    expect(monitor.warning()?.type).toBe('FULLSCREEN_EXIT');
    document.dispatchEvent(new Event('copy', { cancelable: true }));
    expect(monitor.warningCount()).toBe(2);
    document.dispatchEvent(new Event('copy', { cancelable: true }));
    expect(monitor.warningCount()).toBe(3);
    expect(monitor.warning()?.type).toBe('COPY_ATTEMPT');
  });

  // ---- pantalla completa (modo dividido / ventana suelta) ----

  function fullscreenReports(): unknown[] {
    return enqueue.mock.calls.filter((call) => call[0].type === 'FULLSCREEN_EXIT');
  }

  it('exige pantalla completa: sin fullscreenElement reporta FULLSCREEN_EXIT y avisa al alumno', () => {
    start();

    expect(fullscreenReports()).toHaveLength(1);
    expect(enqueue).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'FULLSCREEN_EXIT', result: 'DETECTED' }),
    );
    expect(monitor.warning()?.type).toBe('FULLSCREEN_EXIT');
  });

  it('no repite FULLSCREEN_EXIT hasta la cadencia, y se reinicia al entrar en pantalla completa', () => {
    start();
    expect(fullscreenReports()).toHaveLength(1);

    // El chequeo sigue corriendo (5 s) pero la ventana de reporte es de 30 s.
    vi.advanceTimersByTime(6000);
    expect(fullscreenReports()).toHaveLength(1);

    // Vuelve a pantalla completa: la próxima salida se reporta sin esperar.
    defineFullscreenElement(document.documentElement);
    document.dispatchEvent(new Event('fullscreenchange'));
    expect(fullscreenReports()).toHaveLength(1);

    defineFullscreenElement(null);
    document.dispatchEvent(new Event('fullscreenchange'));
    expect(fullscreenReports()).toHaveLength(2);
  });

  it('si la política no exige pantalla completa, no reporta FULLSCREEN_EXIT', () => {
    getConfig.mockReturnValue(of({ ...DEFAULT_SAFEGUARD_CONFIG, requireFullscreen: false }));
    start();

    expect(fullscreenReports()).toHaveLength(0);
    expect(monitor.warning()).toBeNull();
  });

  it('requestFullscreen() pide pantalla completa al navegador (botón del aviso)', () => {
    start();
    const requestFullscreen = vi.fn(() => Promise.resolve());
    Object.defineProperty(document.documentElement, 'requestFullscreen', {
      configurable: true,
      value: requestFullscreen,
    });

    monitor.requestFullscreen();
    expect(requestFullscreen).toHaveBeenCalledTimes(1);
  });

  it('dismissWarning apaga el aviso actual', () => {
    start();
    document.dispatchEvent(new Event('copy', { cancelable: true }));
    expect(monitor.warning()).not.toBeNull();
    monitor.dismissWarning();
    expect(monitor.warning()).toBeNull();
  });

  it('envía heartbeat periódicamente mientras el examen sigue abierto', () => {
    start();
    vi.advanceTimersByTime(DEFAULT_SAFEGUARD_CONFIG.heartbeatIntervalSeconds * 1000);
    expect(heartbeat).toHaveBeenCalledWith('session-1');
  });

  it('stop() cierra la sesión, sale del modo protegido y deja de escuchar eventos', () => {
    start();
    monitor.stop();

    expect(closeSession).toHaveBeenCalledWith('session-1');
    expect(exitProtectedMode).toHaveBeenCalledTimes(1);
    expect(monitor.active()).toBe(false);

    enqueue.mockClear();
    document.dispatchEvent(new Event('copy', { cancelable: true }));
    expect(enqueue).not.toHaveBeenCalled(); // ya no hay examen: no debe reportar nada
  });

  it('llamar start() dos veces para el mismo intento no abre una segunda sesión', () => {
    start();
    monitor.start('exam-1', 'attempt-1');
    vi.advanceTimersByTime(0);
    expect(startSession).toHaveBeenCalledTimes(1);
  });
});
