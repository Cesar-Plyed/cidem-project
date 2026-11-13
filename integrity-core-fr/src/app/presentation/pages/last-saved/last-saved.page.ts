import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonContent, IonToolbar, IonTitle, IonIcon, IonButton } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { timeOutline, checkmarkDoneCircleOutline } from 'ionicons/icons';
import { AutosaveService } from '../../../application/autosave/autosave.service';

@Component({
  selector: 'app-last-saved',
  templateUrl: 'last-saved.page.html',
  styleUrls: ['last-saved.page.scss'],
  imports: [CommonModule, FormsModule, IonContent, IonToolbar, IonTitle, IonIcon, IonButton],
})
export class LastSavedPage implements OnInit {
  private readonly autosaveService = inject(AutosaveService);

  readonly lastSaved = signal<string | null>(null);
  readonly autosaveFailed = signal(false);

  constructor() {
    addIcons({ timeOutline, checkmarkDoneCircleOutline });
  }

  ngOnInit(): void {
    this.autosaveService.lastSaved$.subscribe(ts => {
      this.lastSaved.set(ts);
    });
    this.autosaveService.saveFailed$.subscribe(failed => {
      this.autosaveFailed.set(failed);
    });
  }

  async retrySave(): Promise<void> {
    await this.autosaveService.manualSave();
  }
}
