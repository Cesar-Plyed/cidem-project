import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { map } from 'rxjs';
import { LegalContentService } from '../../../core/services/legal-content.service';

/**
 * Documentos legales públicos: /legal/aviso-legal, /legal/privacidad,
 * /legal/cookies y /legal/terminos. Accesibles sin sesión.
 */
@Component({
  selector: 'app-legal-doc-page',
  templateUrl: './legal-doc.page.html',
  styleUrls: ['./legal-doc.page.scss'],
  imports: [IonContent, RouterLink],
})
export class LegalDocPage {
  private readonly route = inject(ActivatedRoute);
  private readonly content = inject(LegalContentService);

  private readonly docId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('doc'))),
    { initialValue: null },
  );

  readonly document = computed(() => this.content.getDocument(this.docId()));
  readonly others = computed(() =>
    this.content.getDocuments().filter((doc) => doc.id !== this.docId()),
  );
}
