import { Component, inject, input, signal } from '@angular/core';
import { LegalApiService } from '../../../core/services/legal-api.service';
import { saveBlob } from '../../../core/services/download.util';
import { ExportArtifact } from '../../../domain/models/legal/legal.model';

/** Derecho de acceso: copia completa de datos y export JSON (spec 42). */
@Component({
  selector: 'app-access-request',
  template: `
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="h5">Copia de tus datos personales</h2>
        <p class="text-muted small">Derecho de acceso del Art. 15 GDPR.</p>

        <div class="d-flex gap-2 flex-wrap">
          <button type="button" class="btn btn-outline-primary" (click)="requestCopy()">
            Solicitar copia de datos
          </button>
          <button type="button" class="btn btn-primary" (click)="exportJson()">
            Exportar a JSON
          </button>
        </div>

        @if (sections().length > 0) {
          <p class="mt-3 mb-1 small">Secciones incluidas: {{ sections().join(', ') }}</p>
        }

        @if (lastExport(); as artifact) {
          <p class="small text-muted mb-0" role="status">
            Archivo {{ artifact.format }} generado ({{ artifact.size }} bytes) el
            {{ artifact.generatedAt }}.
          </p>
        }

        @if (error()) {
          <div class="alert alert-danger mt-3 py-2 small" role="alert">{{ error() }}</div>
        }
      </div>
    </section>
  `,
})
export class AccessRequestComponent {
  private readonly api = inject(LegalApiService);

  readonly studentId = input.required<string>();
  readonly sections = signal<string[]>([]);
  readonly lastExport = signal<ExportArtifact | null>(null);
  readonly error = signal<string | null>(null);

  requestCopy(): void {
    this.api.getAccess(this.studentId()).subscribe({
      next: (data) => this.sections.set(Object.keys(data)),
      error: () => this.error.set('No se pudo obtener la copia de datos.'),
    });
  }

  exportJson(): void {
    this.api.exportAccess(this.studentId()).subscribe({
      next: (blob) => this.lastExport.set(
        saveBlob(blob, `datos-personales-${this.studentId()}.json`, 'JSON'),
      ),
      error: () => this.error.set('No se pudo generar el archivo JSON.'),
    });
  }
}
