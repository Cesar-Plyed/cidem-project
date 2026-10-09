import { Component, OnInit, signal, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { GetAttemptResultUseCase } from '../../../application/use-cases/evaluator/get-attempt-result.use-case';
import { BackendExamResult, BackendQuestionResult } from '../../../domain/models/evaluator/evaluator.model';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

@Component({
  selector: 'app-exam-result',
  templateUrl: 'exam-result.page.html',
  styleUrls: ['exam-result.page.scss'],
  imports: [
    CommonModule,
    RouterLink,
    // Shell de la página: cabecera con título, botón atrás y cierre de sesión.
    AppHeaderComponent,
    IonContent,
  ],
})
export class ExamResultPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly getAttemptResultUseCase = inject(GetAttemptResultUseCase);

  readonly result = signal<BackendExamResult | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const attemptId = this.route.snapshot.paramMap.get('attemptId');
    if (!attemptId) {
      this.errorMessage.set('No se encontró el intento.');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.getAttemptResultUseCase.execute(attemptId).subscribe({
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

  scoreColor(score: number, maxScore: number): string {
    if (maxScore === 0) return 'medium';
    const ratio = score / maxScore;
    if (ratio >= 0.7) return 'success';
    if (ratio >= 0.4) return 'warning';
    return 'danger';
  }

  getOptionText(question: BackendQuestionResult, optionId: string): string {
    const opt = question.options?.find((o) => o.id === optionId);
    return opt ? opt.text : optionId;
  }
}
