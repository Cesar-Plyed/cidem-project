import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable, signal } from '@angular/core';
import {
  DEFAULT_SAFEGUARD_CONFIG,
  SafeguardConfig,
  SafeguardEventResult,
  SafeguardEventType,
  SafeguardEvent,
  SafeguardWarning,
} from '../../domain/models/safeguard/safeguard.model';
import { SafeguardPlatform } from '../../domain/ports/safeguard-platform';
import { CloseSafeguardSessionUseCase } from '../use-cases/safeguard/close-safeguard-session.use-case';
import { GetSafeguardConfigUseCase } from '../use-cases/safeguard/get-safeguard-config.use-case';
import { SendSafeguardHeartbeatUseCase } from '../use-cases/safeguard/send-safeguard-heartbeat.use-case';
import { StartSafeguardSessionUseCase } from '../use-cases/safeguard/start-safeguard-session.use-case';
import { SafeguardEventDispatcher } from './safeguard-event-dispatcher';
import { CLIPBOARD_SHORTCUTS, normalizeShortcut, prettyShortcut, shortcutOf } from './safeguard-shortcuts';
import { warningMessageFor } from './safeguard-warning-messages';

/** Espera antes de dar por buena una pérdida de foco (el cambio de pestaña dispara blur y visibilitychange a la vez). */
const BLUR_CONFIRM_MS = 150;
const WARNING_VISIBLE_MS = 6000;

/**
 * Vigila el examen mientras esté abierto: detecta cambio de pestaña y pérdida de foco, y
 * bloquea copiar, cortar, pegar, el menú contextual y los atajos restringidos. Todo eso
 * SOLO entre start() y stop(): fuera del examen el navegador se comporta con normalidad.
 *
 * Uso desde la pantalla del examen:
 *   monitor.start(examId, attemptId);   // al cargar un intento sin entregar
 *   monitor.stop();                     // al entregar o al salir de la pantalla
 */
@Injectable({ providedIn: 'root' })
export class SafeguardMonitorService {
  private readonly doc = inject(DOCUMENT);
  private readonly platform = inject(SafeguardPlatform);
  private readonly dispatcher = inject(SafeguardEventDispatcher);
  private readonly getConfigUseCase = inject(GetSafeguardConfigUseCase);
  private readonly startSessionUseCase = inject(StartSafeguardSessionUseCase);
  private readonly heartbeatUseCase = inject(SendSafeguardHeartbeatUseCase);
  private readonly closeSessionUseCase = inject(CloseSafeguardSessionUseCase);

  private readonly activeState = signal(false);
  private readonly currentWarning = signal<SafeguardWarning | null>(null);
  private readonly warningsTotal = signal(0);
  private readonly heartbeatFailed = signal(false);

  /** true mientras la protección está corriendo. */
  readonly active = this.activeState.asReadonly();
  /** Último aviso para el alumno (se apaga solo a los pocos segundos). */
  readonly warning = this.currentWarning.asReadonly();
  /** Cuántos avisos se han mostrado en este examen. */
  readonly warningCount = this.warningsTotal.asReadonly();
  /** Eventos que aún no llegaron al backend. */
  private readonly pendingBeforeSession = signal(0);
  readonly pendingEvents = computed(() => this.dispatcher.pending() + this.pendingBeforeSession());
  /** true si el backend no responde (latido o envío de eventos fallando). */
  readonly connectionLost = computed(() => this.heartbeatFailed() || this.dispatcher.unreachable());

  private context: { examId: string; attemptId: string } | null = null;
  private config: SafeguardConfig = DEFAULT_SAFEGUARD_CONFIG;
  private restricted = new Set<string>();
  private sessionId: string | null = null;
  private sessionStarting = false;
  /** El servidor rechazó la sesión de forma definitiva: no se vuelve a intentar abrirla. */
  private sessionRejected = false;
  private preSessionEvents: SafeguardEvent[] = [];
  private heartbeatInFlight = false;
  private runId = 0;
  private warningSeq = 0;

  private listeners: AbortController | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private blurTimer: ReturnType<typeof setTimeout> | null = null;
  private warningTimer: ReturnType<typeof setTimeout> | null = null;
  private unsubscribeNative: (() => void) | null = null;
  private readonly lastReportedAt = new Map<string, number>();

  start(examId: string, attemptId: string): void {
    if (this.context) {
      if (this.context.attemptId === attemptId) return;
      this.stop();
    }

    const run = ++this.runId;
    this.context = { examId, attemptId };
    this.sessionId = null;
    this.sessionStarting = false;
    this.sessionRejected = false;
    this.lastReportedAt.clear();
    this.warningsTotal.set(0);
    this.heartbeatFailed.set(false);
    this.dispatcher.resume();
    this.pendingBeforeSession.set(0);

    // La protección arranca YA con la política estricta; la configuración del
    // servidor se aplica cuando llegue, así no hay una ventana sin protección.
    this.setConfig(DEFAULT_SAFEGUARD_CONFIG);
    this.attachListeners();
    this.unsubscribeNative = this.platform.onNativeViolation((violation) =>
      this.report(violation.type, 'DETECTED', violation.detail),
    );
    this.activeState.set(true);

    this.platform.enterProtectedMode().catch((err: unknown) => console.warn('SafeGuard: modo protegido no disponible', err));

    this.getConfigUseCase.execute(examId).subscribe({
      next: (config) => {
        if (!this.isCurrent(run)) return;
        this.setConfig(config);
        if (!config.enabled) {
          this.stop(); // el docente desactivó SafeGuard para este examen
          return;
        }
        this.beginSessionAndHeartbeat(run);
      },
      error: (err: unknown) => {
        if (!this.isCurrent(run)) return;
        if (statusOf(err) === 404) {
          // El backend responde 404 cuando el examen no tiene SafeGuard configurado: no hay
          // nada que vigilar ni sesión que abrir (sessions/start también devolvería 404).
          console.info('SafeGuard: el examen no tiene configuración en el servidor, no se activa la protección.');
          this.stop();
          return;
        }
        console.warn('SafeGuard: no se pudo leer la configuración, se usa la política estricta por defecto.', err);
        this.beginSessionAndHeartbeat(run);
      },
    });
  }

  stop(): void {
    if (!this.context) return;

    const sessionId = this.sessionId;
    this.runId++; // invalida cualquier respuesta de red que llegue después
    this.context = null;
    this.sessionId = null;
    this.preSessionEvents = [];
    this.pendingBeforeSession.set(0);
    this.sessionStarting = false;

    this.listeners?.abort();
    this.listeners = null;
    this.unsubscribeNative?.();
    this.unsubscribeNative = null;
    this.clearHeartbeat();
    this.clearBlurTimer();
    this.clearWarningTimer();

    this.dispatcher.finalize();
    if (sessionId) this.closeQuietly(sessionId);
    this.platform.exitProtectedMode().catch((err: unknown) => console.warn('SafeGuard: no se pudo salir del modo protegido', err));

    this.activeState.set(false);
    this.currentWarning.set(null);
    this.heartbeatFailed.set(false);
  }

  dismissWarning(): void {
    this.clearWarningTimer();
    this.currentWarning.set(null);
  }

  // ---- configuración, sesión y latido ----

  private setConfig(config: SafeguardConfig): void {
    this.config = config;
    this.restricted = new Set(
      config.restrictedShortcuts.map(normalizeShortcut).filter((shortcut): shortcut is string => shortcut !== null),
    );
  }

  private beginSessionAndHeartbeat(run: number): void {
    this.ensureSession(run);
    this.clearHeartbeat();
    this.heartbeatTimer = setInterval(() => this.tick(run), this.config.heartbeatIntervalSeconds * 1000);
  }

  private ensureSession(run: number): void {
    const context = this.context;
    if (!context || this.sessionId || this.sessionStarting || this.sessionRejected) return;

    this.sessionStarting = true;
    this.startSessionUseCase.execute(context.examId, context.attemptId).subscribe({
      next: (session) => {
        if (!this.isCurrent(run)) {
          // El examen ya terminó cuando el servidor respondió: se cierra la sesión que abrió.
          if (session.sessionId) this.closeQuietly(session.sessionId);
          return;
        }
        this.sessionStarting = false;
        this.sessionId = session.sessionId || null;
        this.flushPreSessionEvents();
      },
      error: (err: unknown) => {
        if (!this.isCurrent(run)) return;
        this.sessionStarting = false;
        const status = statusOf(err);
        if (status === 400 || status === 403 || status === 404) {
          // El backend rechazó la sesión de forma definitiva (intento inexistente, ajeno o
          // petición inválida): reintentar cada latido solo generaría errores repetidos.
          console.warn(`SafeGuard: el servidor rechazó abrir la sesión (HTTP ${status}); no se reintenta.`, err);
          this.sessionRejected = true;
          this.clearHeartbeat();
          return;
        }
        // Red, 5xx o 409 por concurrencia: se reintenta en el siguiente latido.
      },
    });
  }

  private tick(run: number): void {
    if (!this.isCurrent(run)) return;
    if (!this.sessionId) {
      this.ensureSession(run);
      return;
    }
    if (this.heartbeatInFlight) return;

    this.heartbeatInFlight = true;
    this.heartbeatUseCase.execute(this.sessionId).subscribe({
      next: () => {
        this.heartbeatInFlight = false;
        if (this.heartbeatFailed()) {
          this.heartbeatFailed.set(false);
          this.dispatcher.flushNow(); // volvió la conexión: se entrega lo pendiente
        }
      },
      error: () => {
        this.heartbeatInFlight = false;
        this.heartbeatFailed.set(true);
      },
    });
  }

  private closeQuietly(sessionId: string): void {
    this.closeSessionUseCase.execute(sessionId).subscribe({ error: () => undefined });
  }

  private isCurrent(run: number): boolean {
    return this.context !== null && this.runId === run;
  }

  // ---- listeners del navegador ----

  private attachListeners(): void {
    const win = this.doc.defaultView;
    if (!win) return;

    const controller = new AbortController();
    this.listeners = controller;
    // capture: se intercepta antes que cualquier componente de la página.
    const capture = { capture: true, signal: controller.signal };

    this.doc.addEventListener('visibilitychange', () => this.onVisibilityChange(), capture);
    // blur va SIN capture: así solo llega el de la ventana y no el de cada campo de texto.
    win.addEventListener('blur', (event) => this.onWindowBlur(event, win), { signal: controller.signal });
    win.addEventListener('online', () => this.dispatcher.flushNow(), { signal: controller.signal });

    this.doc.addEventListener('copy', (event) => this.block(event, 'COPY_ATTEMPT', this.config.blockCopy), capture);
    this.doc.addEventListener('cut', (event) => this.block(event, 'CUT_ATTEMPT', this.config.blockCut), capture);
    this.doc.addEventListener('paste', (event) => this.block(event, 'PASTE_ATTEMPT', this.config.blockPaste), capture);
    this.doc.addEventListener(
      'contextmenu',
      (event) => this.block(event, 'CONTEXT_MENU', this.config.blockContextMenu),
      capture,
    );
    this.doc.addEventListener('keydown', (event) => this.onKeyDown(event), capture);
  }

  private onVisibilityChange(): void {
    if (this.config.detectTabSwitch && this.doc.visibilityState === 'hidden') {
      this.report('TAB_HIDDEN', 'DETECTED');
    }
  }

  private onWindowBlur(event: Event, win: Window): void {
    if (!this.config.detectFocusLoss || event.target !== win) return;

    this.clearBlurTimer();
    this.blurTimer = setTimeout(() => {
      this.blurTimer = null;
      // Si la pestaña quedó oculta ya se reportó TAB_HIDDEN; si el foco volvió, fue un parpadeo.
      if (!this.context || this.doc.hidden || this.doc.hasFocus()) return;
      this.report('WINDOW_BLUR', 'DETECTED');
    }, BLUR_CONFIRM_MS);
  }

  private block(event: Event, type: SafeguardEventType, enabled: boolean): void {
    if (!enabled) return;
    event.preventDefault();
    this.report(type, 'BLOCKED');
  }

  private onKeyDown(event: KeyboardEvent): void {
    if (!this.config.blockRestrictedShortcuts) return;

    const shortcut = shortcutOf(event);
    if (!shortcut || !this.restricted.has(shortcut)) return;

    event.preventDefault();
    if (event.repeat) return; // mantener la tecla pulsada no debe contar como intentos nuevos

    const clipboardType = CLIPBOARD_SHORTCUTS[shortcut];
    if (clipboardType) {
      this.report(clipboardType, 'BLOCKED');
    } else {
      this.report('RESTRICTED_SHORTCUT', 'BLOCKED', prettyShortcut(shortcut));
    }
  }

  // ---- reporte y avisos ----

  private report(type: SafeguardEventType, result: SafeguardEventResult, detail?: string): void {
    const context = this.context;
    if (!context) return;

    // El aviso al alumno sale siempre; al backend solo se reporta una vez por ventana de throttling.
    this.showWarning(type, detail);

    const key = `${type}:${detail ?? ''}`;
    const now = Date.now();
    const last = this.lastReportedAt.get(key);
    if (last !== undefined && now - last < this.config.eventThrottleMs) return;
    this.lastReportedAt.set(key, now);

    const safeguardEvent: SafeguardEvent = {
      eventId: newEventId(),
      examId: context.examId,
      attemptId: context.attemptId,
      sessionId: this.sessionId ?? '',
      type,
      result,
      occurredAt: new Date(now).toISOString(),
      detail,
    };

    // El backend rechaza eventos sin sessionId. Si todavía estamos esperando
    // /sessions/start, se conservan localmente y se entregan apenas exista la sesión.
    if (this.sessionId) {
      this.dispatcher.enqueue(safeguardEvent);
    } else {
      this.preSessionEvents.push(safeguardEvent);
      this.pendingBeforeSession.set(this.preSessionEvents.length);
    }
  }

  private flushPreSessionEvents(): void {
    if (!this.sessionId || this.preSessionEvents.length === 0) return;

    const sessionId = this.sessionId;
    const pending = this.preSessionEvents;
    this.preSessionEvents = [];
    this.pendingBeforeSession.set(0);

    for (const event of pending) {
      this.dispatcher.enqueue({ ...event, sessionId });
    }
  }

  private showWarning(type: SafeguardEventType, detail?: string): void {
    this.currentWarning.set({ id: ++this.warningSeq, type, message: warningMessageFor(type, detail) });
    this.warningsTotal.update((total) => total + 1);
    this.clearWarningTimer();
    this.warningTimer = setTimeout(() => this.dismissWarning(), WARNING_VISIBLE_MS);
  }

  private clearHeartbeat(): void {
    if (this.heartbeatTimer !== null) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
    this.heartbeatInFlight = false;
  }

  private clearBlurTimer(): void {
    if (this.blurTimer !== null) {
      clearTimeout(this.blurTimer);
      this.blurTimer = null;
    }
  }

  private clearWarningTimer(): void {
    if (this.warningTimer !== null) {
      clearTimeout(this.warningTimer);
      this.warningTimer = null;
    }
  }
}

function statusOf(err: unknown): number | null {
  const status = (err as { status?: unknown } | null)?.status;
  return typeof status === 'number' ? status : null;
}

/**
 * crypto.randomUUID solo existe en contextos seguros (https/localhost); con http
 * por IP hay que reemplazarlo. El id solo correlaciona eventos del monitor
 * (no firma ni autentica nada), así que el respaldo con Math.random() es
 * aceptable donde no hay crypto: NOSONAR S2245
 */
function newEventId(): string {
  const cryptoApi = globalThis.crypto as Crypto | undefined;
  if (typeof cryptoApi?.randomUUID === 'function') return cryptoApi.randomUUID();

  const bytes = new Uint8Array(16);
  if (cryptoApi?.getRandomValues) {
    cryptoApi.getRandomValues(bytes);
  } else {
    for (let i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256); // NOSONAR S2245
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
