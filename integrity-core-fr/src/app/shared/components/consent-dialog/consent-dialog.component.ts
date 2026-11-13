import { Component, input, output, signal, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import { LegalApiService } from '../../../core/services/legal-api.service';
import {
  CONSENT_TYPES,
  ConsentType,
} from '../../../domain/models/legal/legal.model';

/** Modal de consentimiento que se muestra al iniciar un examen (spec 36). */
@Component({
  selector: 'app-consent-dialog',
  template: `
    @if (visible()) {
      <div class="consent-backdrop" role="dialog" aria-modal="true" aria-labelledby="consent-title">
        <div class="consent-dialog card shadow">
          <div class="card-body">
            <h2 id="consent-title" class="h5">Consentimiento para la monitorizacion</h2>
            <p class="text-muted small">
              Necesitamos tu consentimiento para las funciones de integridad academica.
              Puedes revocarlo en cualquier momento.
            </p>

            @for (type of types; track type) {
              <div class="form-check mb-2">
                <input class="form-check-input" type="checkbox" [id]="'consent-' + type"
                       [checked]="selection()[type]" (change)="toggle(type, $event)" />
                <label class="form-check-label" [attr.for]="'consent-' + type">{{ label(type) }}</label>
              </div>
            }

            @if (message()) {
              <div class="alert alert-info py-2 small" role="status">{{ message() }}</div>
            }

            <div class="d-flex gap-2 justify-content-end mt-3">
              <button type="button" class="btn btn-outline-secondary" [disabled]="saving()"
                      (click)="revokeSelected()">Revocar</button>
              <button type="button" class="btn btn-primary" [disabled]="saving()"
                      (click)="accept()">Aceptar</button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .consent-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.45); display: grid; place-items: center; z-index: 1050; padding: 1rem; }
    .consent-dialog { max-width: 32rem; width: 100%; }
  `],
})
export class ConsentDialogComponent {
  private readonly api = inject(LegalApiService);

  readonly studentId = input.required<string>();
  readonly visible = input(true);
  readonly closed = output<void>();

  readonly types = CONSENT_TYPES;
  readonly selection = signal<Record<ConsentType, boolean>>({
    CAMERA: false,
    MICROPHONE: false,
    SCREENSHOT: false,
    TAB_MONITORING: false,
  });
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);

  toggle(type: ConsentType, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selection.update((current) => ({ ...current, [type]: checked }));
  }

  label(type: ConsentType): string {
    const labels: Record<ConsentType, string> = {
      CAMERA: 'Camara',
      MICROPHONE: 'Microfono',
      SCREENSHOT: 'Capturas de pantalla',
      TAB_MONITORING: 'Monitorizacion de pestanas',
    };
    return labels[type];
  }

  selectedTypes(): ConsentType[] {
    return this.types.filter((type) => this.selection()[type]);
  }

  accept(): void {
    const selected = this.selectedTypes();
    if (selected.length === 0) {
      this.message.set('Selecciona al menos un consentimiento.');
      return;
    }
    this.saving.set(true);
    forkJoin(selected.map((type) => this.api.grantConsent(this.studentId(), type))).subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set('Consentimientos otorgados.');
        this.closed.emit();
      },
      error: () => {
        this.saving.set(false);
        this.message.set('No se pudieron guardar los consentimientos.');
      },
    });
  }

  revokeSelected(): void {
    const selected = this.selectedTypes();
    if (selected.length === 0) {
      this.message.set('Selecciona al menos un consentimiento.');
      return;
    }
    this.saving.set(true);
    forkJoin(selected.map((type) => this.api.revokeConsent(this.studentId(), type))).subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set('Consentimientos revocados.');
        this.closed.emit();
      },
      error: () => {
        this.saving.set(false);
        this.message.set('No se pudieron revocar los consentimientos.');
      },
    });
  }
}
