import { Component, OnInit, signal, inject, OnDestroy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { GetExamUseCase } from '../../../application/use-cases/exam/get-exam.use-case';
import { SubmitExamAttemptUseCase } from '../../../application/use-cases/exam/submit-exam-attempt.use-case';
import { Exam } from '../../../domain/models/exam/exam.model';
import { QuestionType } from '../../../domain/models/exam/question-type.enum';
import { ExamAnswer } from '../../../domain/models/exam/exam-answer.model';
import { SafeguardMonitorService } from '../../../application/safeguard/safeguard-monitor.service';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';
import { LegalNoticeComponent } from '../../../shared/components/legal-notice/legal-notice.component';

@Component({
  selector: 'app-exam-take',
  templateUrl: 'exam-take.page.html',
  styleUrls: ['exam-take.page.scss'],
  imports: [
    ReactiveFormsModule,
    // Shell de la página: cabecera con título, botón atrás y cierre de sesión.
    AppHeaderComponent,
    LegalNoticeComponent,
    IonContent,
  ],
})
export class ExamTakePage implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder)
  private readonly route = inject(ActivatedRoute)
  private readonly getExamUseCase = inject(GetExamUseCase)
  private readonly submitExamAttemptUseCase = inject(SubmitExamAttemptUseCase)
  /** Público: la plantilla muestra sus avisos y su estado. */
  readonly safeguard = inject(SafeguardMonitorService)

  readonly QuestionType = QuestionType;

  // Signals: sin zone.js, Angular solo refresca la vista cuando cambia un signal.
  readonly exam = signal<Exam | null>(null);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** El examen no se muestra hasta aceptar el aviso legal (spec 37). */
  readonly legalAccepted = signal(false);

  form: FormGroup = this.fb.group({});

  onLegalRejected(): void {
    // El propio aviso redirige a /login; aquí solo se mantiene el examen bloqueado.
    this.legalAccepted.set(false);
  }

  constructor(
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage.set('Examen no encontrado.');
      this.loading.set(false);
      return;
    }

    this.getExamUseCase.execute(id).subscribe({
      next: (exam) => {
        if (!exam) {
          this.errorMessage.set('Examen no encontrado.');
          this.loading.set(false);
          return;
        }
        this.buildAnswerForm(exam); // el form debe existir antes de pintar el examen
        this.exam.set(exam);
        this.loading.set(false);
        // Iniciar SafeGuard si el examen no está entregado
        this.safeguard.start(exam.id, id);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar el examen', err.status, err.error);
        this.errorMessage.set(`No se pudo cargar el examen (HTTP ${err.status}).`);
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.safeguard.stop();
  }

  private buildAnswerForm(exam: Exam): void {
    const group: Record<string, unknown> = {};
    exam.questions.forEach((question) => {
      group[question.id] =
        question.type === QuestionType.MULTIPLE_CHOICE
          ? [null, Validators.required]
          : ['', Validators.required];
    });
    this.form = this.fb.group(group);
  }

  submit(): void {
    const exam = this.exam();
    if (!exam || this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Responde todas las preguntas antes de enviar.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue() as Record<string, string | null>;
    const answers: ExamAnswer[] = exam.questions.map((question) => ({
      questionId: question.id,
      selectedOptionId:
        question.type === QuestionType.MULTIPLE_CHOICE ? raw[question.id] ?? undefined : undefined,
      openText: question.type === QuestionType.OPEN ? raw[question.id] ?? undefined : undefined,
    }));

    this.submitExamAttemptUseCase.execute(exam.id, answers).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
        this.safeguard.stop();
      },
      error: () => {
        this.submitting.set(false);
        this.errorMessage.set('No se pudo enviar el examen. Intenta de nuevo.');
      },
    });
  }
}
