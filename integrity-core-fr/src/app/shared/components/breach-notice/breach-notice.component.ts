import { Component, OnInit, inject, input, signal } from '@angular/core';
import { LegalApiService } from '../../../core/services/legal-api.service';
import { BreachNotice } from '../../../domain/models/legal/legal.model';

/** Notificaciones de brechas de datos al estudiante (spec 49). */
@Component({
  selector: 'app-breach-notice',
  template: `
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="h5">Avisos de seguridad</h2>
        <button type="button" class="btn btn-outline-secondary btn-sm mb-3" (click)="load()">
          Ver notificaciones de brecha
        </button>

        @for (notice of notifications(); track notice.id) {
          <article class="border rounded p-2 mb-2">
            <header class="d-flex justify-content-between">
              <strong>{{ notice.breachType }}</strong>
              <span class="badge" [class]="notice.status === 'RESOLVED' ? 'text-bg-success' : 'text-bg-warning'">
                {{ notice.status }}
              </span>
            </header>
            <p class="mb-1 small">{{ notice.description }}</p>
            <p class="mb-1 small text-muted">{{ notice.recommendedAction }}</p>
            <p class="mb-2 small text-muted">Notificado el {{ notice.notifiedAt }}</p>
            @if (notice.status !== 'RESOLVED') {
              <button type="button" class="btn btn-sm btn-primary" (click)="resolve(notice.id)">
                Marcar como resuelto
              </button>
            }
          </article>
        } @empty {
          <p class="text-muted small mb-0">No hay notificaciones pendientes.</p>
        }

        @if (message()) {
          <div class="alert alert-info mt-3 py-2 small" role="status">{{ message() }}</div>
        }
      </div>
    </section>
  `,
})
export class BreachNoticeComponent implements OnInit {
  private readonly api = inject(LegalApiService);

  readonly studentId = input.required<string>();
  readonly notifications = signal<BreachNotice[]>([]);
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.getPendingBreaches(this.studentId()).subscribe({
      next: (response) => this.notifications.set(response.notifications),
      error: () => this.message.set('No se pudieron cargar las notificaciones.'),
    });
  }

  resolve(notificationId: string): void {
    this.api.resolveBreach(notificationId).subscribe({
      next: () => {
        this.message.set('Notificacion marcada como resuelta.');
        this.load();
      },
      error: () => this.message.set('No se pudo marcar la notificacion como resuelta.'),
    });
  }
}
