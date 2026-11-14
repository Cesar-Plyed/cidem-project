import { inject, Injectable, signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { SessionService } from '../session/session.service';

/**
 * Servicio de autosave: maneja el estado de conexión, último guardado,
 * intentos fallidos y guardado manual para los componentes de UI de spec 27.
 */
@Injectable({ providedIn: 'root' })
export class AutosaveService {

  private readonly sessionService = inject(SessionService);

  // BehaviorSubjects para suscripción
  public readonly connectionState$ = new BehaviorSubject<ConnectionState>({
    connected: true,
    attempts: 0,
    lastReconnection: null,
  });
  public readonly lastSaved$ = new BehaviorSubject<string | null>(null);
  public readonly saveFailed$ = new BehaviorSubject<boolean>(false);
  public readonly recoveryMessage$ = new BehaviorSubject<string | null>(null);

  // Signals para los componentes
  readonly connectionState = signal<ConnectionState>({
    connected: true,
    attempts: 0,
    lastReconnection: null,
  });
  readonly lastSaved = signal<string | null>(null);
  readonly saveFailed = signal(false);
  readonly recoveryMessage = signal<string | null>(null);

  private connectionSubject = new BehaviorSubject<ConnectionState>({
    connected: true,
    attempts: 0,
    lastReconnection: null,
  });

  /** Simula pérdida de conexión para pruebas de UI. */
  simulateDisconnect(): void {
    this.connectionSubject.next({ ...this.connectionState(), connected: false, attempts: this.connectionState().attempts + 1 });
    this.connectionState.set(this.connectionSubject.getValue());
  }

  /** Simula reconexión. */
  async reconnect(): Promise<void> {
    this.connectionSubject.next({
      connected: true,
      attempts: this.connectionState().attempts,
      lastReconnection: new Date().toISOString(),
    });
    this.connectionState.set(this.connectionSubject.getValue());
    this.recoveryMessage.set('Conexión restaurada correctamente.');
    setTimeout(() => this.recoveryMessage.set(null), 5000);
  }

  /** Guarda el último timestamp de autosave exitoso. */
  markSaved(timestamp: string): void {
    this.lastSaved.set(timestamp);
  }

  /** Marca un fallo de autosave. */
  markFailed(): void {
    this.saveFailed.set(true);
    this.recoveryMessage.set('Autosave falló. Reintentando...');
  }

  /** Simula guardado manual (para el botón "Reintentar"). */
  async manualSave(): Promise<void> {
    this.saveFailed.set(false);
    this.markSaved(new Date().toISOString());
    this.recoveryMessage.set('Guardado manual completado.');
  }

  /** Limpia el mensaje de recuperación. */
  clearRecoveryMessage(): void {
    this.recoveryMessage.set(null);
  }

  getConnectionState(): ConnectionState {
    return this.connectionState();
  }

  getAutosaveIntervalMs(): number {
    return 30_000; // 30 segundos
  }
}

export interface ConnectionState {
  connected: boolean;
  attempts: number;
  lastReconnection: string | null;
}
