import { inject, Injectable, signal } from '@angular/core';
import { SafeguardEvent } from '../../domain/models/safeguard/safeguard.model';
import { RegisterSafeguardEventUseCase } from '../use-cases/safeguard/register-safeguard-event.use-case';

export interface SafeguardDispatcherOptions {
  /** Pausa entre un envío exitoso y el siguiente: evita saturar el backend con ráfagas. */
  minIntervalMs: number;
  retryBaseMs: number;
  retryMaxMs: number;
  /** Tope de la cola; al excederlo se descarta el evento más antiguo. */
  maxQueueSize: number;
  /** Tras terminar el examen, cuánto se sigue intentando entregar lo pendiente. */
  finalizeGraceMs: number;
}

const DEFAULT_OPTIONS: SafeguardDispatcherOptions = {
  minIntervalMs: 500,
  retryBaseMs: 1000,
  retryMaxMs: 30_000,
  maxQueueSize: 500,
  finalizeGraceMs: 60_000,
};

/**
 * Envía los eventos de SafeGuard al backend de uno en uno y en orden.
 *
 * - Si el envío falla por red o por el servidor (status 0, 408, 429, 5xx), el evento
 *   se queda en la cola y se reintenta con espera exponencial (1s, 2s, 4s... máx. 30s).
 * - Si el servidor lo rechaza (otro 4xx) el evento se descarta: reintentarlo para
 *   siempre bloquearía todos los que vienen detrás.
 * - La cola vive en memoria: si el alumno recarga la página con eventos sin enviar, se pierden.
 */
@Injectable({ providedIn: 'root' })
export class SafeguardEventDispatcher {
  private readonly registerEvent = inject(RegisterSafeguardEventUseCase);

  private options: SafeguardDispatcherOptions = { ...DEFAULT_OPTIONS };
  private queue: SafeguardEvent[] = [];
  private sending = false;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private failedAttempts = 0;
  private deadline: number | null = null;

  private readonly pendingCount = signal(0);
  private readonly serverUnreachable = signal(false);

  /** Eventos que todavía no se han podido entregar. */
  readonly pending = this.pendingCount.asReadonly();
  /** true mientras el último intento de envío falló por red o por el servidor. */
  readonly unreachable = this.serverUnreachable.asReadonly();

  configure(options: Partial<SafeguardDispatcherOptions>): void {
    this.options = { ...this.options, ...options };
  }

  enqueue(event: SafeguardEvent): void {
    this.queue.push(event);
    if (this.queue.length > this.options.maxQueueSize) {
      this.queue.shift();
      console.warn('SafeGuard: cola de eventos llena, se descartó el más antiguo.');
    }
    this.pendingCount.set(this.queue.length);
    if (this.timer === null && !this.sending) {
      this.schedule(0);
    }
  }

  /** Reintenta ya (por ejemplo, cuando el navegador avisa que volvió la red). */
  flushNow(): void {
    this.failedAttempts = 0;
    if (this.sending || this.queue.length === 0) return;
    this.schedule(0);
  }

  /** El examen terminó: se sigue intentando entregar lo pendiente, pero con plazo. */
  finalize(): void {
    this.deadline = Date.now() + this.options.finalizeGraceMs;
    this.flushNow();
  }

  /** Empieza un examen nuevo: se cancela el plazo de un finalize anterior. */
  resume(): void {
    this.deadline = null;
  }

  /** Vacía todo (para pruebas o para un cierre de sesión completo). */
  reset(): void {
    this.clearTimer();
    this.queue = [];
    this.sending = false;
    this.failedAttempts = 0;
    this.deadline = null;
    this.pendingCount.set(0);
    this.serverUnreachable.set(false);
  }

  private schedule(delayMs: number): void {
    this.clearTimer();
    this.timer = setTimeout(() => this.drain(), delayMs);
  }

  private clearTimer(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  private drain(): void {
    this.timer = null;
    const event = this.queue[0];
    if (this.sending || !event) return;

    this.sending = true;
    this.registerEvent.execute(event).subscribe({
      next: () => this.onDelivered(),
      error: (err: unknown) => this.onFailed(err),
    });
  }

  private onDelivered(): void {
    this.sending = false;
    this.queue.shift();
    this.failedAttempts = 0;
    this.serverUnreachable.set(false);
    this.pendingCount.set(this.queue.length);
    if (this.queue.length > 0) {
      this.schedule(this.options.minIntervalMs);
    }
  }

  private onFailed(err: unknown): void {
    this.sending = false;

    if (!isRetryable(err)) {
      console.warn('SafeGuard: el servidor rechazó un evento, se descarta.', err);
      this.queue.shift();
      this.failedAttempts = 0;
      this.serverUnreachable.set(false);
      this.pendingCount.set(this.queue.length);
      if (this.queue.length > 0) {
        this.schedule(this.options.minIntervalMs);
      }
      return;
    }

    this.serverUnreachable.set(true);
    if (this.deadline !== null && Date.now() >= this.deadline) {
      console.warn(`SafeGuard: se agotó el plazo, ${this.queue.length} evento(s) sin entregar.`);
      this.queue = [];
      this.pendingCount.set(0);
      return;
    }

    this.failedAttempts += 1;
    const backoff = this.options.retryBaseMs * 2 ** (this.failedAttempts - 1);
    this.schedule(Math.min(backoff, this.options.retryMaxMs));
  }
}

function isRetryable(err: unknown): boolean {
  const status = (err as { status?: unknown } | null)?.status;
  if (typeof status !== 'number') return true;
  return status === 0 || status === 408 || status === 429 || status >= 500;
}
