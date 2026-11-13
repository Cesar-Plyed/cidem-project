import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonContent, IonToolbar, IonTitle, IonIcon, IonButton } from '@ionic/angular';
import { addIcons } from 'ionicons';
import {
  warningOutline,
  cloudOfflineOutline,
  cloudDownloadOutline,
  refreshOutline,
} from 'ionicons/icons';
import { AutosaveService } from '../../../application/autosave/autosave.service';

@Component({
  selector: 'app-recovery-notice',
  templateUrl: 'recovery-notice.page.html',
  styleUrls: ['recovery-notice.page.scss'],
  imports: [CommonModule, FormsModule, IonContent, IonToolbar, IonTitle, IonIcon, IonButton],
})
export class RecoveryNoticePage implements OnInit {
  private readonly autosaveService = inject(AutosaveService);

  readonly recoveryMessage = signal<string | null>(null);
  readonly connectionLost = signal(false);
  readonly title = signal('');
  readonly description = signal('');
  readonly showAlert = signal(false);

  constructor() {
    addIcons({ warningOutline, cloudOfflineOutline, cloudDownloadOutline, refreshOutline });
  }

  ngOnInit(): void {
    this.autosaveService.recoveryMessage$.subscribe(msg => {
      if (msg) {
        this.recoveryMessage.set(msg);
        this.showAlert.set(true);
      } else {
        this.showAlert.set(false);
      }
    });
  }

  getAlertContent(): RecoveryAlert {
    const state = this.autosaveService.getConnectionState();
    if (!state.connected) {
      return {
        title: 'Conexión perdida',
        description: 'Se perdió la conexión con el servidor. Puedes intentar restaurar tu progreso.',
      };
    }
    return {
      title: 'Autosave disponible',
      description: 'Tu progreso se guardó correctamente.',
    };
  }

  retry(): void {
    this.autosaveService.markFailed();
    this.connectionLost.set(false);
    this.autosaveService.manualSave();
  }

  reconnect(): void {
    this.autosaveService.reconnect();
    this.connectionLost.set(false);
  }
}

export interface RecoveryAlert {
  title: string;
  description: string;
}
