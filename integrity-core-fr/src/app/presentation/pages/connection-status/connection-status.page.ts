import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonContent, IonToolbar, IonTitle, IonIcon, IonButton } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { checkmarkCircleOutline, closeCircleOutline, refreshOutline } from 'ionicons/icons';
import { AutosaveService, ConnectionState } from '../../../application/autosave/autosave.service';

@Component({
  selector: 'app-connection-status',
  templateUrl: 'connection-status.page.html',
  styleUrls: ['connection-status.page.scss'],
  imports: [CommonModule, FormsModule, IonContent, IonToolbar, IonTitle, IonIcon, IonButton],
})
export class ConnectionStatusPage implements OnInit {
  private readonly autosaveService = inject(AutosaveService);

  readonly isConnected = signal(true);
  readonly lastReconnectionDate = signal<string | null>(null);
  readonly connectionAttempts = signal(0);

  constructor() {
    addIcons({ checkmarkCircleOutline, closeCircleOutline, refreshOutline });
  }

  ngOnInit(): void {
    this.autosaveService.connectionState$.subscribe((state: ConnectionState) => {
      this.isConnected.set(state.connected);
      if (state.connected) {
        this.lastReconnectionDate.set(state.lastReconnection);
        this.connectionAttempts.set(state.attempts);
      }
    });
  }

  async reconnect(): Promise<void> {
    await this.autosaveService.reconnect();
  }
}
