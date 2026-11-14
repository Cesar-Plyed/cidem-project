import { Component, inject, input, signal } from '@angular/core';
import { LegalApiService } from '../../../core/services/legal-api.service';

/** Derecho de rectificacion y borrado logico (spec 43). */
@Component({
  selector: 'app-data-rectification',
  template: `
    <section class="card mb-3">
      <div class="card-body">
        <h2 class="h5">Rectificar mis datos</h2>
        <p class="text-muted small">Art. 16 GDPR: corrige datos inexactos o solicita su borrado logico.</p>

        <div class="mb-2">
          <label class="form-label" for="rect-email">Correo electronico</label>
          <input id="rect-email" class="form-control" type="email"
                 [value]="fields()['email']" [disabled]="!editing()"
                 (input)="update('email', $event)" />
        </div>
        <div class="mb-2">
          <label class="form-label" for="rect-first">Nombre</label>
          <input id="rect-first" class="form-control" type="text"
                 [value]="fields()['firstNames']" [disabled]="!editing()"
                 (input)="update('firstNames', $event)" />
        </div>
        <div class="mb-2">
          <label class="form-label" for="rect-last">Apellidos</label>
          <input id="rect-last" class="form-control" type="text"
                 [value]="fields()['lastNames']" [disabled]="!editing()"
                 (input)="update('lastNames', $event)" />
        </div>

        <div class="d-flex gap-2 mt-3">
          <button type="button" class="btn btn-outline-secondary" [disabled]="editing()" (click)="startEdit()">
            Editar
          </button>
          <button type="button" class="btn btn-primary" [disabled]="!editing()" (click)="save()">
            Guardar
          </button>
          <button type="button" class="btn btn-outline-danger" (click)="remove()">Eliminar</button>
        </div>

        @if (message()) {
          <div class="alert alert-info mt-3 py-2 small" role="status">{{ message() }}</div>
        }
      </div>
    </section>
  `,
})
export class DataRectificationComponent {
  private readonly api = inject(LegalApiService);

  readonly entityId = input.required<string>();
  readonly entityType = input('USER');
  readonly editing = signal(false);
  readonly fields = signal<Record<string, string>>({
    email: '',
    firstNames: '',
    lastNames: '',
  });
  readonly message = signal<string | null>(null);

  update(field: string, event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.fields.update((current) => ({ ...current, [field]: value }));
  }

  startEdit(): void {
    this.editing.set(true);
  }

  save(): void {
    this.api.rectifyData(this.entityType(), this.entityId(), { ...this.fields() }).subscribe({
      next: () => {
        this.editing.set(false);
        this.message.set('Datos rectificados. El cambio queda registrado en el audit trail.');
      },
      error: () => this.message.set('No se pudieron rectificar los datos.'),
    });
  }

  remove(): void {
    this.api.softDeleteData(this.entityType(), this.entityId()).subscribe({
      next: () => this.message.set('Datos marcados como eliminados (borrado logico).'),
      error: () => this.message.set('No se pudieron eliminar los datos.'),
    });
  }
}
