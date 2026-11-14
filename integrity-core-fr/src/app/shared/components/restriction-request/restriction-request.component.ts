import { Component, OnInit, inject, input, signal } from '@angular/core';
import { LegalApiService } from '../../../core/services/legal-api.service';
import { RestrictionStatus } from '../../../domain/models/legal/legal.model';

/** Derecho de limitacion del tratamiento (spec 47). */
@Component({
  selector: 'app-restriction-request',
  template: `
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="h5">Limitar el tratamiento de mis datos</h2>
        <p class="text-muted small">Art. 18 GDPR. Mientras este activa, no se procesan nuevos datos.</p>

        @if (status(); as current) {
          <p class="small">
            Estado:
            <span class="badge" [class]="current.restricted ? 'text-bg-warning' : 'text-bg-secondary'">
              {{ current.restricted ? 'activa' : (current.status ?? 'sin restriccion') }}
            </span>
            @if (current.restrictionType) {
              <span class="ms-2">{{ current.restrictionType }}</span>
            }
          </p>
        }

        <div class="mb-2">
          <label class="form-label" for="restriction-reason">Motivo</label>
          <textarea id="restriction-reason" class="form-control" rows="2"
                    [value]="reason()" (input)="reason.set($any($event.target).value)"></textarea>
        </div>

        <div class="d-flex gap-2">
          <button type="button" class="btn btn-primary" (click)="requestRestriction()">
            Solicitar restriccion
          </button>
          <button type="button" class="btn btn-outline-danger" [disabled]="!status()?.restrictionId"
                  (click)="revokeRestriction()">
            Revocar restriccion
          </button>
        </div>

        @if (message()) {
          <div class="alert alert-info mt-3 py-2 small" role="status">{{ message() }}</div>
        }
      </div>
    </section>
  `,
})
export class RestrictionRequestComponent implements OnInit {
  private readonly api = inject(LegalApiService);

  readonly studentId = input.required<string>();
  readonly status = signal<RestrictionStatus | null>(null);
  readonly reason = signal('');
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.getRestrictionStatus(this.studentId()).subscribe({
      next: (status) => this.status.set(status),
      error: () => this.message.set('No se pudo consultar el estado de la restriccion.'),
    });
  }

  requestRestriction(): void {
    this.api.submitRestriction(this.studentId(), 'PROCESSING', this.reason()).subscribe({
      next: () => {
        this.message.set('Restriccion solicitada. El tratamiento queda limitado.');
        this.load();
      },
      error: () => this.message.set('No se pudo solicitar la restriccion.'),
    });
  }

  revokeRestriction(): void {
    const restrictionId = this.status()?.restrictionId;
    if (!restrictionId) {
      return;
    }
    this.api.revokeRestriction(restrictionId).subscribe({
      next: () => {
        this.message.set('Restriccion revocada.');
        this.load();
      },
      error: () => this.message.set('No se pudo revocar la restriccion.'),
    });
  }
}
