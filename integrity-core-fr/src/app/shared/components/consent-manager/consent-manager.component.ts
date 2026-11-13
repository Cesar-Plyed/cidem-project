import { Component, OnInit, inject, input, signal } from '@angular/core';
import { LegalApiService } from '../../../core/services/legal-api.service';
import {
  ConsentHistoryEntry,
  ConsentStatus,
  ConsentType,
} from '../../../domain/models/legal/legal.model';

/** Tabla de consentimientos + historial append-only (spec 48). */
@Component({
  selector: 'app-consent-manager',
  template: `
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="h5">Consentimientos</h2>

        <div class="table-responsive">
          <table class="table table-sm align-middle">
            <thead>
              <tr>
                <th scope="col">Tipo</th>
                <th scope="col">Estado</th>
                <th scope="col">Otorgado</th>
                <th scope="col">Revocado</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (consent of consents(); track consent.type) {
                <tr>
                  <td>{{ consent.type }}</td>
                  <td>
                    <span class="badge"
                          [class]="consent.status === 'granted' ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ consent.status }}
                    </span>
                  </td>
                  <td>{{ consent.grantedAt ?? '-' }}</td>
                  <td>{{ consent.revokedAt ?? '-' }}</td>
                  <td class="d-flex gap-1">
                    <button type="button" class="btn btn-sm btn-outline-success"
                            (click)="grant(consent.type)">Otorgar</button>
                    <button type="button" class="btn btn-sm btn-outline-danger"
                            (click)="revoke(consent.type)">Revocar</button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="5" class="text-muted small">Sin consentimientos registrados.</td></tr>
              }
            </tbody>
          </table>
        </div>

        <h3 class="h6 mt-4">Historial de cambios</h3>
        <ol class="small mb-0">
          @for (entry of history(); track entry.timestamp + entry.type + entry.action) {
            <li>{{ entry.timestamp }} - {{ entry.action }} {{ entry.type }}</li>
          } @empty {
            <li class="text-muted">Sin cambios registrados.</li>
          }
        </ol>

        @if (message()) {
          <div class="alert alert-info mt-3 py-2 small" role="status">{{ message() }}</div>
        }
      </div>
    </section>
  `,
})
export class ConsentManagerComponent implements OnInit {
  private readonly api = inject(LegalApiService);

  readonly studentId = input.required<string>();
  readonly consents = signal<ConsentStatus[]>([]);
  readonly history = signal<ConsentHistoryEntry[]>([]);
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.api.getConsentStatus(this.studentId()).subscribe({
      next: (overview) => this.consents.set(overview.consents),
      error: () => this.message.set('No se pudo cargar la tabla de consentimientos.'),
    });
    this.api.getConsentHistory(this.studentId()).subscribe({
      next: (history) => this.history.set(history.history),
      error: () => this.message.set('No se pudo cargar el historial.'),
    });
  }

  grant(type: ConsentType): void {
    this.api.grantConsent(this.studentId(), type).subscribe({
      next: () => {
        this.message.set(`Consentimiento ${type} otorgado.`);
        this.refresh();
      },
      error: () => this.message.set(`No se pudo otorgar ${type}.`),
    });
  }

  revoke(type: ConsentType): void {
    this.api.revokeConsent(this.studentId(), type).subscribe({
      next: () => {
        this.message.set(`Consentimiento ${type} revocado.`);
        this.refresh();
      },
      error: () => this.message.set(`No se pudo revocar ${type}.`),
    });
  }
}
