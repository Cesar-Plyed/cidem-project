import { Component, inject, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { LegalContentService } from '../../../core/services/legal-content.service';

/** Aviso legal Art. 13-14 GDPR/LOPDGDD previo al examen (spec 37). */
@Component({
  selector: 'app-legal-notice',
  template: `
    <section class="legal-notice card border-primary mb-3" aria-labelledby="legal-title">
      <div class="card-body">
        <h2 id="legal-title" class="h5">{{ notice.title }}</h2>

        <dl class="row small mb-3">
          <dt class="col-sm-3">Responsable</dt>
          <dd class="col-sm-9">{{ notice.responsible }}</dd>
          <dt class="col-sm-3">Finalidad</dt>
          <dd class="col-sm-9">{{ notice.purpose }}</dd>
          <dt class="col-sm-3">Base legal</dt>
          <dd class="col-sm-9">{{ notice.legalBasis }}</dd>
          <dt class="col-sm-3">Contacto DPO</dt>
          <dd class="col-sm-9"><a [href]="'mailto:' + notice.contact">{{ notice.contact }}</a></dd>
        </dl>

        <h3 class="h6">Derechos</h3>
        <ul class="small">
          @for (right of notice.rights; track right) {
            <li>{{ right }}</li>
          }
        </ul>

        <h3 class="h6">Informacion obligatoria</h3>
        <ul class="small text-muted">
          @for (article of notice.articles; track article) {
            <li>{{ article }}</li>
          }
        </ul>

        <div class="d-flex gap-2 justify-content-end">
          <button type="button" class="btn btn-outline-danger" (click)="reject()">Rechazo</button>
          <button type="button" class="btn btn-primary" (click)="accept()">Acepto</button>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .legal-notice { max-width: 48rem; margin-inline: auto; }
  `],
})
export class LegalNoticeComponent {
  private readonly content = inject(LegalContentService);
  private readonly router = inject(Router);

  readonly notice = this.content.getLegalNotice();
  readonly accepted = output<void>();
  readonly rejected = output<void>();

  /** true cuando el alumno acepta: habilita el inicio del examen. */
  readonly acceptedState = signal(false);

  accept(): void {
    this.acceptedState.set(true);
    this.accepted.emit();
  }

  reject(): void {
    this.acceptedState.set(false);
    this.rejected.emit();
    void this.router.navigate(['/login']);
  }
}
