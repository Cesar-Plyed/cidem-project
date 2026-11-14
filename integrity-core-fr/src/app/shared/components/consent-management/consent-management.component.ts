import { Component, OnInit, inject, input, signal } from '@angular/core';
import { LegalApiService } from '../../../core/services/legal-api.service';
import {
  CONSENT_TYPES,
  ConsentOverview,
  ConsentType,
} from '../../../domain/models/legal/legal.model';

/** Gestion de consentimientos con revocacion selectiva o total (spec 40). */
@Component({
  selector: 'app-consent-management',
  template: `
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="h5">Gestion de consentimientos</h2>
        <p class="text-muted small">
          Puedes revocar un consentimiento o todos a la vez. La revocacion detiene
          inmediatamente la captura correspondiente.
        </p>

        @for (consent of consents(); track consent.type) {
          <div class="d-flex align-items-center justify-content-between border-bottom py-2">
            <span>
              <strong>{{ consent.type }}</strong>
              <span class="badge ms-2" [class]="consent.status === 'granted' ? 'text-bg-success' : 'text-bg-secondary'">
                {{ consent.status }}
              </span>
            </span>
            <button type="button" class="btn btn-sm btn-outline-danger"
                    [disabled]="consent.status !== 'granted'"
                    (click)="revoke(consent.type)">Revocar {{ consent.type }}</button>
          </div>
        }

        <div class="d-flex justify-content-end mt-3">
          <button type="button" class="btn btn-danger" (click)="revokeAll()">Revocar todo</button>
        </div>

        @if (message()) {
          <div class="alert alert-info mt-3 py-2 small" role="status">{{ message() }}</div>
        }
      </div>
    </section>
  `,
})
export class ConsentManagementComponent implements OnInit {
  private readonly api = inject(LegalApiService);

  readonly studentId = input.required<string>();
  readonly consents = signal<ConsentOverview['consents']>([]);
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.getConsentStatus(this.studentId()).subscribe({
      next: (overview) => this.consents.set(overview.consents),
      error: () => this.message.set('No se pudo cargar el estado de los consentimientos.'),
    });
  }

  revoke(type: ConsentType): void {
    this.api.revokeConsent(this.studentId(), type).subscribe({
      next: () => {
        this.message.set(`Consentimiento ${type} revocado.`);
        this.load();
      },
      error: () => this.message.set(`No se pudo revocar ${type}.`),
    });
  }

  revokeAll(): void {
    this.api.revokeAllConsents(this.studentId()).subscribe({
      next: () => {
        this.message.set('Todos los consentimientos fueron revocados.');
        this.load();
      },
      error: () => this.message.set('No se pudieron revocar los consentimientos.'),
    });
  }

  get types(): ConsentType[] {
    return CONSENT_TYPES;
  }
}
