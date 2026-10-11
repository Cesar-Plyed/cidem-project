import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { IonContent } from '@ionic/angular';
import { GetStudentExamHistoryUseCase } from '../../../application/use-cases/evaluator/get-student-exam-history.use-case';
import { StudentExamHistoryEntry } from '../../../domain/models/evaluator/evaluator.model';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

@Component({
  selector: 'app-exam-history',
  templateUrl: './exam-history.page.html',
  standalone: true,
  imports: [DatePipe, RouterLink, IonContent, AppHeaderComponent],
})
export class ExamHistoryPage implements OnInit {
  private readonly getHistory = inject(GetStudentExamHistoryUseCase);

  readonly entries = signal<StudentExamHistoryEntry[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.getHistory.execute().subscribe({
      next: (entries) => {
        this.entries.set(entries);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(`No se pudo cargar tu historial (HTTP ${err.status}).`);
        this.loading.set(false);
      },
    });
  }
}
