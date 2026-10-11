import { computed, inject, Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SessionService } from '../../application/session/session.service';
import { Role } from '../../domain/models/role.enum';
import { TokenStorageService } from '../storage/token-storage.service';
import { SentinelAlertNotification, SentinelEventMessage } from './sentinel-message.model';

/**
 * Cliente STOMP para Sentinel (/ws/sentinel).
 *
 * Solo es relevante para ALUMNOS: es la vía por la que el navegador reporta
 * foco perdido / cambio de pestaña a Sentinel, que los agrega en ventana de
 * tiempo y genera alertas (deduplicadas en Redis). El resto de roles no
 * necesita conexión y no se conecta.
 *
 * - Conecta perezosamente (la primera vez que se publica un evento) con el token
 *   de acceso en el header Authorization (el interceptor del backend lo exige).
 * - Si el socket cae, stompjs reintenta con backoff por sí solo.
 * - publish() nunca lanza: si no hay sesión/conexión el evento simplemente se
 *   cae (SafeGuard REST es el canal primario de reporte; este es complementario).
 */
@Injectable({ providedIn: 'root' })
export class SentinelStompService {
  private readonly sessionService = inject(SessionService);
  private readonly tokenStorage = inject(TokenStorageService);

  private client: Client | null = null;
  private connecting: Promise<void> | null = null;
  private alertsSubscription: StompSubscription | null = null;
  private examStatusSubscription: StompSubscription | null = null;

  private readonly connectedState = signal(false);
  private readonly lastAlert = signal<SentinelAlertNotification | null>(null);
  private readonly closedAttemptId = signal<string | null>(null);

  readonly connected = this.connectedState.asReadonly();
  /** Última alerta que Sentinel devolvió por /user/queue/sentinel/alerts. */
  readonly alert = this.lastAlert.asReadonly();
  readonly closedAttempt = this.closedAttemptId.asReadonly();
  /** true si hay sesión activa de ALUMNO: el único rol que reporta a Sentinel. */
  readonly shouldReport = computed(async () => (await this.sessionService.getRole()) === Role.ALUMNO);

  /** Publica un evento; resuelve rápido y no lanza en fallo de conexión. */
  async publish(event: SentinelEventMessage): Promise<void> {
    if ((await this.sessionService.getRole()) !== Role.ALUMNO) return;

    try {
      await this.ensureConnected();
      this.client?.publish({
        destination: '/app/sentinel/events',
        body: JSON.stringify(event),
      });
    } catch {
      // Sin conexión a Sentinel el examen sigue: es un canal complementario.
      console.warn('Sentinel: no se pudo publicar el evento por WebSocket (se ignora).');
    }
  }

  async connectForExam(): Promise<void> {
    if ((await this.sessionService.getRole()) !== Role.ALUMNO) return;
    await this.ensureConnected();
  }

  clearClosedAttempt(attemptId: string): void {
    if (this.closedAttemptId() === attemptId) this.closedAttemptId.set(null);
  }

  /** Cierra la conexión (al cerrar sesión o al terminar la app). */
  async disconnect(): Promise<void> {
    this.alertsSubscription?.unsubscribe();
    this.alertsSubscription = null;
    this.examStatusSubscription?.unsubscribe();
    this.examStatusSubscription = null;
    const client = this.client;
    this.client = null;
    this.connecting = null;
    this.connectedState.set(false);
    if (client?.connected) {
      await client.deactivate().catch(() => undefined);
    }
  }

  private ensureConnected(): Promise<void> {
    if (this.client?.connected) return Promise.resolve();
    if (this.connecting) return this.connecting;

    this.connecting = this.connect().catch((err: unknown) => {
      console.warn('Sentinel: conexión WebSocket fallida (se reintenta en el próximo evento).', err);
      this.connecting = null;
      throw err;
    });
    return this.connecting;
  }

  private async connect(): Promise<void> {
    const token = await this.tokenStorage.getAccessToken();
    if (!token) throw new Error('Sin token de acceso');

    const client = new Client({
      brokerURL: `${environment.apiUrl.replace(/^http/, 'ws')}/ws/sentinel`,
      // Reintento ante caídas: 5 s base, máx. 30 s.
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        this.connectedState.set(true);
        this.subscribeToAlerts(client);
        this.subscribeToExamStatus(client);
      },
      onWebSocketClose: () => this.connectedState.set(false),
      onStompError: (frame) => console.warn('Sentinel: error STOMP', frame.headers['message']),
    });

    this.client = client;
    client.activate();
    // stompjs no expone una promesa de "conectado"; esperamos al primer onConnect
    // con un plazo prudente para no bloquear indefinidamente la publicación.
    await this.waitForConnection(client, 5000);
  }

  private waitForConnection(client: Client, timeoutMs: number): Promise<void> {
    return new Promise((resolve, reject) => {
      const started = Date.now();
      const poll = setInterval(() => {
        if (client.connected) {
          clearInterval(poll);
          resolve();
        } else if (Date.now() - started > timeoutMs) {
          clearInterval(poll);
          reject(new Error('Sentinel: la conexión no llegó a establecerse'));
        }
      }, 100);
    });
  }

  private subscribeToAlerts(client: Client): void {
    this.alertsSubscription?.unsubscribe();
    this.alertsSubscription = client.subscribe('/user/queue/sentinel/alerts', (message: IMessage) => {
      try {
        this.lastAlert.set(JSON.parse(message.body) as SentinelAlertNotification);
      } catch {
        console.warn('Sentinel: alerta con cuerpo no JSON, se ignora.');
      }
    });
  }

  private subscribeToExamStatus(client: Client): void {
    this.examStatusSubscription?.unsubscribe();
    this.examStatusSubscription = client.subscribe('/user/queue/exam-status', (message: IMessage) => {
      try {
        const notification = JSON.parse(message.body) as { attemptId?: unknown };
        if (typeof notification.attemptId === 'string') {
          this.closedAttemptId.set(notification.attemptId);
        } else {
          console.warn('Sentinel: notificación de cierre sin attemptId, se ignora.');
        }
      } catch {
        console.warn('Sentinel: notificación de cierre con cuerpo no JSON, se ignora.');
      }
    });
  }
}
