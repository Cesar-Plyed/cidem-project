import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { IonContent, AlertController } from '@ionic/angular';
import { ListOpenEndedReviewsUseCase } from '../../../application/use-cases/evaluator/list-open-ended-reviews.use-case';
import { SubmitReviewUseCase } from '../../../application/use-cases/evaluator/submit-review.use-case';
import { GetExamAverageUseCase } from '../../../application/use-cases/evaluator/get-exam-average.use-case';
import { BackendOpenEndedReview } from '../../../domain/models/evaluator/evaluator.model';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

interface ReviewForm {
  score: number;
  feedback: string;
}

@Component({
  selector: 'app-exam-reviews',
  templateUrl: 'exam-reviews.page.html',
  styleUrls: ['exam-reviews.page.scss'],
  imports: [IonContent, AppHeaderComponent],
})
export class ExamReviewsPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly alertController = inject(AlertController);
  private readonly listReviewsUseCase = inject(ListOpenEndedReviewsUseCase);
  private readonly submitReviewUseCase = inject(SubmitReviewUseCase);
  private readonly getExamAverageUseCase = inject(GetExamAverageUseCase);

  readonly reviews = signal<BackendOpenEndedReview[]>([]);
  readonly loading = signal(true);
  readonly submittingId = signal<string | null>(null);
  readonly average = signal<{
    averageScore: number;
    maxScore: number;
    averagePercentage: number;
    totalAttempts: number;
    gradedAttempts: number;
    pendingReviewCount: number;
  } | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly pendingCount = computed(() => this.reviews().filter((r) => !r.reviewed).length);
  readonly reviewedCount = computed(() => this.reviews().filter((r) => r.reviewed).length);

  private formMap = signal<Record<string, ReviewForm>>({});

  ngOnInit(): void {
    const examId = this.route.snapshot.paramMap.get('examId');
    if (!examId) {
      this.errorMessage.set('No se encontró el examen.');
      this.loading.set(false);
      return;
    }
    this.loadReviews(examId);
    this.loadAverage(examId);
  }

  private loadReviews(examId: string): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.listReviewsUseCase.execute(examId, false).subscribe({
      next: (reviews) => {
        this.reviews.set(reviews);
        const forms: Record<string, ReviewForm> = {};
        reviews.forEach((r) => {
          forms[r.answerId] = {
            score: r.score ?? 0,
            feedback: r.feedback ?? '',
          };
        });
        this.formMap.set(forms);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(`No se pudieron cargar las revisiones (HTTP ${err.status}).`);
        this.loading.set(false);
      },
    });
  }

  private loadAverage(examId: string): void {
    this.getExamAverageUseCase.execute(examId).subscribe({
      next: (avg) => {
        this.average.set({
          averageScore: avg.averageScore,
          maxScore: avg.maxScore,
          averagePercentage: avg.averagePercentage,
          totalAttempts: avg.totalAttempts,
          gradedAttempts: avg.gradedAttempts,
          pendingReviewCount: avg.pendingReviewCount,
        });
      },
      error: () => {
        this.average.set(null);
      },
    });
  }

  getScore(review: BackendOpenEndedReview): number {
    return this.formMap()[review.answerId]?.score ?? review.score ?? 0;
  }

  getFeedback(review: BackendOpenEndedReview): string {
    return this.formMap()[review.answerId]?.feedback ?? review.feedback ?? '';
  }

  onScoreInput(review: BackendOpenEndedReview, value: string | null | undefined): void {
    const num = value ? Number(value) : 0;
    const points = review.questionPoints ?? Number.MAX_SAFE_INTEGER;
    // El backend rechaza puntajes sobre los puntos de la pregunta: se acota aquí
    // para no llegar al error.
    const score = num >= 0 ? Math.min(num, points) : 0;
    this.formMap.update((forms) => ({
      ...forms,
      [review.answerId]: {
        score,
        feedback: forms[review.answerId]?.feedback ?? review.feedback ?? '',
      },
    }));
  }

  onFeedbackInput(review: BackendOpenEndedReview, value: string | null | undefined): void {
    this.formMap.update((forms) => ({
      ...forms,
      [review.answerId]: {
        score: forms[review.answerId]?.score ?? review.score ?? 0,
        feedback: value ?? '',
      },
    }));
  }

  async submitReview(review: BackendOpenEndedReview): Promise<void> {
    const form = this.formMap()[review.answerId];
    if (!form || form.score < 0) {
      const alert = await this.alertController.create({
        header: 'Puntuación inválida',
        message: 'La puntuación no puede ser negativa.',
        buttons: [{ text: 'OK' }],
      });
      await alert.present();
      await alert.onDidDismiss();
      return;
    }

    this.submittingId.set(review.answerId);
    this.submitReviewUseCase.execute(review.answerId, form.score, form.feedback || undefined).subscribe({
      next: () => {
        this.submittingId.set(null);
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.submittingId.set(null);
        const detail = (err.error as { message?: string } | null)?.message;
        this.errorMessage.set(`No se pudo guardar la revisión (HTTP ${err.status}).${detail ? ' ' + detail : ''}`);
      },
    });
  }

  private reload(): void {
    const examId = this.route.snapshot.paramMap.get('examId');
    if (examId) {
      this.loadReviews(examId);
      this.loadAverage(examId);
    }
  }
}
