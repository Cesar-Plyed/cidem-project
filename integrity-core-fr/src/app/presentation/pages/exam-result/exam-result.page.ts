import { Component, OnInit, signal, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { GetAttemptResultUseCase } from '../../../application/use-cases/evaluator/get-attempt-result.use-case';
import { SubmitReviewUseCase } from '../../../application/use-cases/evaluator/submit-review.use-case';
import { BackendExamResult, BackendQuestionResult } from '../../../domain/models/evaluator/evaluator.model';
import { SessionService } from '../../../application/session/session.service';
import { Role } from '../../../domain/models/role.enum';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

@Component({
  selector: 'app-exam-result',
  templateUrl: 'exam-result.page.html',
  styleUrls: ['exam-result.page.scss'],
  imports: [
    RouterLink,
    // Shell de la página: cabecera con título, botón atrás y cierre de sesión.
    AppHeaderComponent,
    IonContent,
  ],
})
export class ExamResultPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly getAttemptResultUseCase = inject(GetAttemptResultUseCase);
  private readonly submitReviewUseCase = inject(SubmitReviewUseCase);
  private readonly sessionService = inject(SessionService);

  readonly result = signal<BackendExamResult | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  /** Solo el profesor/administrativo califica: el alumno nunca ve el formulario. */
  readonly canGrade = signal(false);
  /** Pregunta abierta que se está calificando en este momento (null = ninguna). */
  readonly editingQuestionId = signal<string | null>(null);
  readonly gradingScore = signal(0);
  readonly gradingFeedback = signal('');
  readonly gradingSaving = signal(false);
  readonly gradingError = signal<string | null>(null);

  private attemptId: string | null = null;

  ngOnInit(): void {
    this.attemptId = this.route.snapshot.paramMap.get('attemptId');
    void this.sessionService.getRole().then((role) => {
      this.canGrade.set(role === Role.PROFESOR || role === Role.ADMINISTRATIVO);
    });
    if (!this.attemptId) {
      this.errorMessage.set('No se encontró el intento.');
      this.loading.set(false);
      return;
    }
    this.loadResult();
  }

  private loadResult(): void {
    if (!this.attemptId) return;
    this.loading.set(true);
    this.getAttemptResultUseCase.execute(this.attemptId).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          `No se pudo cargar el resultado (HTTP ${err.status}).`,
        );
        this.loading.set(false);
      },
    });
  }

  /** Aprobado desde 60%; por debajo se reproba (la nota del examen es sobre 10). */
  scoreColor(score: number, maxScore: number): string {
    if (maxScore === 0) return 'medium';
    return score / maxScore >= 0.6 ? 'success' : 'danger';
  }

  getOptionText(question: BackendQuestionResult, optionId: string): string {
    const opt = question.options?.find((o) => o.id === optionId);
    return opt ? opt.text : optionId;
  }

  /** Abre el formulario de calificación para una pregunta abierta. */
  startGrading(question: BackendQuestionResult): void {
    this.editingQuestionId.set(question.questionId);
    this.gradingScore.set(Math.min(question.score ?? 0, question.points));
    this.gradingFeedback.set(question.feedback ?? '');
    this.gradingError.set(null);
  }

  cancelGrading(): void {
    this.editingQuestionId.set(null);
    this.gradingError.set(null);
  }

  onGradingScoreInput(value: string | null | undefined): void {
    const num = value === null || value === undefined || value === '' ? 0 : Number(value);
    this.gradingScore.set(Number.isFinite(num) ? Math.max(0, num) : 0);
  }

  onGradingFeedbackInput(value: string | null | undefined): void {
    this.gradingFeedback.set(value ?? '');
  }

  /** Guarda la puntuación/feedback de la respuesta abierta y recarga el resultado. */
  submitGrade(question: BackendQuestionResult): void {
    if (this.gradingSaving()) return;
    if (!question.answerId) {
      this.gradingError.set('Esta respuesta no tiene identificador para calificar.');
      return;
    }
    const score = this.gradingScore();
    if (score > question.points) {
      this.gradingError.set(
        `La puntuación no puede superar los ${question.points} puntos de la pregunta.`,
      );
      return;
    }

    this.gradingSaving.set(true);
    this.gradingError.set(null);
    this.submitReviewUseCase
      .execute(question.answerId, score, this.gradingFeedback() || undefined)
      .subscribe({
        next: () => {
          this.gradingSaving.set(false);
          this.editingQuestionId.set(null);
          // El resultado se recalcula en el backend: se vuelve a leer completo.
          this.loadResult();
        },
        error: (err: HttpErrorResponse) => {
          this.gradingSaving.set(false);
          const detail = (err.error as { message?: string } | null)?.message;
          this.gradingError.set(
            `No se pudo guardar la calificación (HTTP ${err.status}).${detail ? ' ' + detail : ''}`,
          );
        },
      });
  }
}
