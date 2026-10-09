import { Component, OnInit, signal, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, switchMap } from 'rxjs';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { IonContent, IonIcon, AlertController } from '@ionic/angular';
import { addIcons } from 'ionicons';
import { addOutline, copyOutline, createOutline, keyOutline, trashOutline, eyeOutline, readerOutline, statsChartOutline } from 'ionicons/icons';
import { ListExamsUseCase } from '../../../application/use-cases/exam/list-exams.use-case';
import { DeleteExamUseCase } from '../../../application/use-cases/exam/delete-exam.use-case';
import { GenerateAccessCodeUseCase } from '../../../application/use-cases/evaluator/generate-access-code.use-case';
import { PublishRemoteExamUseCase } from '../../../application/use-cases/evaluator/publish-remote-exam.use-case';
import { BackendExamAccessCode } from '../../../domain/models/evaluator/evaluator.model';
import { Exam } from '../../../domain/models/exam/exam.model';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

/** Minutos de vigencia del código de acceso (el backend usa 20 si no se envía). */
const CODE_TTL_MINUTES = 60;

@Component({
  selector: 'app-exam-list',
  templateUrl: 'exam-list.page.html',
  styleUrls: ['exam-list.page.scss'],
  imports: [
    CommonModule,
    RouterLink,
    IonContent,
    IonIcon,
    AppHeaderComponent,
  ],
})
export class ExamListPage implements OnInit {
  private readonly listExamsUseCase = inject(ListExamsUseCase)
  private readonly deleteExamUseCase = inject(DeleteExamUseCase)
  private readonly alertController = inject(AlertController)
  private readonly generateAccessCodeUseCase = inject(GenerateAccessCodeUseCase)
  private readonly publishRemoteExamUseCase = inject(PublishRemoteExamUseCase)

  // Signals: sin zone.js, Angular solo refresca la vista cuando cambia un signal.
  readonly exams = signal<Exam[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  // Códigos generados en esta sesión, por id de examen. El backend no tiene un
  // endpoint para consultar códigos ya emitidos, así que solo se conservan aquí.
  readonly codes = signal<Record<string, BackendExamAccessCode>>({});
  readonly generatingId = signal<string | null>(null);
  readonly copiedId = signal<string | null>(null);
  private confirming = false;

  constructor(
  ) {
    addIcons({ addOutline, copyOutline, createOutline, keyOutline, trashOutline, eyeOutline, readerOutline, statsChartOutline });
  }

  ngOnInit(): void {
    this.loadExams();
  }

  loadExams(showSpinner = true): void {
    if (showSpinner) this.loading.set(true);
    this.errorMessage.set(null);
    this.listExamsUseCase.execute().subscribe({
      next: (exams) => {
        this.exams.set(exams);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar los exámenes', err.status, err.error);
        this.errorMessage.set(`No se pudieron cargar los exámenes (HTTP ${err.status}).`);
        this.loading.set(false);
      },
    });
  }

  statusLabel(exam: Exam): string {
    switch (exam.status) {
      case 'PUBLISHED': return 'Publicado';
      case 'CLOSED': return 'Cerrado';
      default: return 'Borrador';
    }
  }

  statusColor(exam: Exam): string {
    switch (exam.status) {
      case 'PUBLISHED': return 'success';
      case 'CLOSED': return 'medium';
      default: return 'warning';
    }
  }

  /** Clase Bootstrap del badge de estado (traduce los colores Ionic de statusColor). */
  statusBadgeClass(exam: Exam): string {
    switch (this.statusColor(exam)) {
      case 'success': return 'text-bg-success';
      case 'warning': return 'text-bg-warning';
      default: return 'text-bg-secondary';
    }
  }

  /** Botón "código": si el examen ya está publicado genera el código; si no, pide confirmar la publicación. */
  async requestCode(exam: Exam): Promise<void> {
    if (this.generatingId() || this.confirming) return; // un solo código a la vez
    if (exam.status === 'CLOSED') {
      this.errorMessage.set('El examen está cerrado: ya no admite códigos de acceso.');
      return;
    }
    if (exam.status === 'PUBLISHED') {
      this.generateCode(exam, false);
      return;
    }

    this.confirming = true;
    const alert = await this.alertController.create({
      header: 'Publicar examen',
      message:
        'Para generar un código el examen debe publicarse. Después ya no podrás editarlo ni quitarle preguntas. ¿Continuar?',
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Publicar y generar', handler: () => this.generateCode(exam, true) },
      ],
    });
    await alert.present();
    await alert.onDidDismiss();
    this.confirming = false;
  }

  private generateCode(exam: Exam, publishFirst: boolean): void {
    if (this.generatingId()) return;
    this.errorMessage.set(null);
    this.generatingId.set(exam.id);

    const generate$ = this.generateAccessCodeUseCase.execute(exam.id, CODE_TTL_MINUTES);
    const request$ = publishFirst
      ? this.publishRemoteExamUseCase.execute(exam.id).pipe(switchMap(() => generate$))
      : generate$;

    request$.pipe(finalize(() => this.generatingId.set(null))).subscribe({
      next: (result) => {
        this.codes.update((codes) => ({ ...codes, [exam.id]: result }));
        this.exams.update((exams) =>
          exams.map((item) => (item.id === exam.id ? { ...item, status: 'PUBLISHED' as const } : item)),
        );
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al generar el código', err.status, err.error);
        const detail = (err.error as { message?: string } | null)?.message;
        this.errorMessage.set(`No se pudo generar el código (HTTP ${err.status}).${detail ? ' ' + detail : ''}`);
        this.loadExams(false); // el examen pudo haberse publicado aunque el código falló
      },
    });
  }

  async copyCode(examId: string): Promise<void> {
    const code = this.codes()[examId]?.code;
    if (!code) return;
    try {
      await navigator.clipboard.writeText(code);
      this.copiedId.set(examId);
      setTimeout(() => this.copiedId.set(null), 1500);
    } catch {
      this.errorMessage.set('No se pudo copiar. Selecciona el código y cópialo manualmente.');
    }
  }

  async confirmDelete(exam: Exam): Promise<void> {
    const alert = await this.alertController.create({
      header: 'Eliminar examen',
      message: `¿Seguro que quieres eliminar "${exam.title}"? Esta acción no se puede deshacer.`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Eliminar', role: 'destructive', handler: () => this.deleteExam(exam.id) },
      ],
    });
    await alert.present();
  }

  private deleteExam(id: string): void {
    this.deleteExamUseCase.execute(id).subscribe({
      next: () => this.loadExams(),
      error: (err: HttpErrorResponse) =>
        this.errorMessage.set(`No se pudo eliminar el examen (HTTP ${err.status}).`),
    });
  }

  onNavigate(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    if (target) {
      target.blur();
    }
  }
}
