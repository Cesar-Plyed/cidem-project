import { Component, inject, input, signal } from '@angular/core';
import { LegalApiService } from '../../../core/services/legal-api.service';
import { saveBlob } from '../../../core/services/download.util';
import { ExportArtifact } from '../../../domain/models/legal/legal.model';

/** Derecho a la portabilidad en JSON o CSV (spec 45). */
@Component({
  selector: 'app-portability-export',
  template: `
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="h5">Exportar mis datos</h2>
        <p class="text-muted small">Art. 20 GDPR: descarga portable y legible por humanos.</p>

        <div class="d-flex gap-2 flex-wrap">
          <button type="button" class="btn btn-outline-primary" (click)="exportAs('json')">
            Exportar como JSON
          </button>
          <button type="button" class="btn btn-outline-primary" (click)="exportAs('csv')">
            Exportar como CSV
          </button>
        </div>

        @if (lastExport(); as artifact) {
          <p class="small text-muted mt-3 mb-0" role="status">
            {{ artifact.format }} generado ({{ artifact.size }} bytes) el {{ artifact.generatedAt }}.
          </p>
        }

        @if (error()) {
          <div class="alert alert-danger mt-3 py-2 small" role="alert">{{ error() }}</div>
        }
      </div>
    </section>
  `,
})
export class PortabilityExportComponent {
  private readonly api = inject(LegalApiService);

  readonly studentId = input.required<string>();
  readonly lastExport = signal<ExportArtifact | null>(null);
  readonly error = signal<string | null>(null);

  exportAs(format: 'json' | 'csv'): void {
    this.api.exportPortability(this.studentId(), format).subscribe({
      next: (blob) => this.lastExport.set(
        saveBlob(blob, `portabilidad-${this.studentId()}.${format}`, format.toUpperCase()),
      ),
      error: () => this.error.set(`No se pudo exportar el archivo ${format.toUpperCase()}.`),
    });
  }
}
