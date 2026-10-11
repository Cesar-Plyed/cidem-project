import { inject, Injectable } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { environment } from '../../../environments/environment';
import { TokenStorageService } from '../storage/token-storage.service';

@Injectable({ providedIn: 'root' })
export class OversightStompService {
  private readonly tokenStorage = inject(TokenStorageService);

  subscribeToExam(examId: string, onEvent: () => void): () => void {
    let disposed = false;
    let subscription: StompSubscription | null = null;
    let client: Client | null = null;

    void this.tokenStorage.getAccessToken().then((token) => {
      if (disposed) return;
      if (!token) throw new Error('No hay token de acceso para abrir supervisión en tiempo real');
      client = new Client({
        brokerURL: `${environment.apiUrl.replace(/^http/, 'ws')}/ws/sentinel`,
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        connectHeaders: { Authorization: `Bearer ${token}` },
        onConnect: () => {
          if (disposed || !client) return;
          subscription?.unsubscribe();
          subscription = client.subscribe(`/topic/oversight/exams/${examId}`, onEvent);
        },
        onStompError: (frame) => console.warn('Supervisión en tiempo real: error STOMP', frame.headers['message']),
        onWebSocketClose: () => console.warn('Supervisión en tiempo real desconectada; se conserva la actualización periódica.'),
      });
      client.activate();
    }).catch((err: unknown) => {
      if (!disposed) console.warn('No se pudo iniciar la supervisión en tiempo real.', err);
    });

    return () => {
      disposed = true;
      subscription?.unsubscribe();
      if (client?.active) void client.deactivate();
    };
  }
}
