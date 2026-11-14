import { Component, OnInit, inject, input, signal } from '@angular/core';
import { LegalApiService } from '../../../core/services/legal-api.service';
import { saveBlob } from '../../../core/services/download.util';
import {
  DpiaSummary,
  ExportArtifact,
} from '../../../domain/models/legal/legal.model';

/** Revision de EIPD/DPIA por el DPO (spec 50). */
@Component({
  selector: 'app-dpia-review',
  template: `
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="h5">Evaluaciones de impacto (EIPD)</h2>
        <p class="text-muted small">Art. 35 GDPR. Pendientes de revision por el DPO.</p>

        @for (dpia of pending(); track dpia.id) {
          <article class="border rounded p-2 mb-2">
            <header class="d-flex justify-content-between">
              <strong>{{ dpia.title }}</strong>
              <span class="badge text-bg-secondary">{{ dpia.riskLevel }}</span>
            </header>
            <p class="small text-muted mb-2">Estado: {{ dpia.status }}</p>
            <div class="d-flex gap-2 flex-wrap">
              <button type="button" class="btn btn-sm btn-success" (click)="review(dpia.id, 'APPROVED')">
                Aprobar
              </button>
              <button type="button" class="btn btn-sm btn-outline-danger" (click)="review(dpia.id, 'REJECTED')">
                Rechazar
              </button>
              <button type="button" class="btn btn-sm btn-outline-primary" (click)="downloadPdf(dpia.id)">
                Descargar PDF
              </button>
            </div>
          </article>
        } @empty {
          <p class="text-muted small mb-0">No hay EIPD pendientes de revision.</p>
        }

        @if (lastExport(); as artifact) {
          <p class="small text-muted mt-3 mb-0" role="status">
            PDF generado ({{ artifact.size }} bytes) el {{ artifact.generatedAt }}.
          </p>
        }

        @if (message()) {
          <div class="alert alert-info mt-3 py-2 small" role="status">{{ message() }}</div>
        }
      </div>
    </section>
  `,
})
export class DpiaReviewComponent implements OnInit {
  private readonly api = inject(LegalApiService);

  readonly studentId = input.required<string>();
  readonly pending = signal<DpiaSummary[]>([]);
  readonly lastExport = signal<ExportArtifact | null>(null);
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.getPendingDpias(this.studentId()).subscribe({
      next: (list) => this.pending.set(list.dpias),
      error: () => this.message.set('No se pudieron cargar las EIPD pendientes.'),
    });
  }

  review(dpiId: string, status: 'APPROVED' | 'REJECTED'): void {
    this.api.reviewDpia(dpiId, status).subscribe({
      next: () => {
        this.message.set(`EIPD ${status === 'APPROVED' ? 'aprobada' : 'rechazada'}.`);
        this.load();
      },
      error: () => this.message.set('No se pudo registrar la revision.'),
    });
  }

  downloadPdf(dpiId: string): void {
    this.api.downloadDpiaPdf(dpiId).subscribe({
      next: (blob) => this.lastExport.set(
        saveBlob(blob, `eipd-${dpiId}.pdf`, 'PDF'),
      ),
      error: () => this.message.set('No se pudo descargar el PDF.'),
    });
  }
}
