import { Component, OnInit, signal, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { IonContent } from '@ionic/angular';
import { BackendExamResult } from '../../../domain/models/evaluator/evaluator.model';
import { GetExamResultsUseCase } from '../../../application/use-cases/evaluator/get-exam-results.use-case';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

@Component({
  selector: 'app-exam-results',
  templateUrl: 'exam-results.page.html',
  styleUrls: ['exam-results.page.scss'],
  imports: [CommonModule, RouterLink, IonContent, AppHeaderComponent],
})
export class ExamResultsPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly getExamResultsUseCase = inject(GetExamResultsUseCase);

  readonly results = signal<BackendExamResult[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  examId: string | null = null;

  ngOnInit(): void {
    this.examId = this.route.snapshot.paramMap.get('examId');
    if (!this.examId) {
      this.errorMessage.set('No se encontró el examen.');
      this.loading.set(false);
      return;
    }
    this.loadResults(this.examId);
  }

  private loadResults(examId: string): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.getExamResultsUseCase.execute(examId).subscribe({
      next: (results) => {
        this.results.set(results);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          `No se pudieron cargar los resultados (HTTP ${err.status}).`,
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

  scorePercentage(score: number, maxScore: number): number {
    if (maxScore === 0) return 0;
    return Math.round((score / maxScore) * 100);
  }

  onNavigate(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    if (target) {
      target.blur();
    }
  }
}
