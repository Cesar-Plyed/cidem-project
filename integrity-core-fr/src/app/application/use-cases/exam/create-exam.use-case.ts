import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExamRepository } from '../../../domain/ports/exam.repository';
import { Exam } from '../../../domain/models/exam/exam.model';

@Injectable({ providedIn: 'root' })
export class CreateExamUseCase {
  private readonly examRepository = inject(ExamRepository)

  execute(exam: Omit<Exam, 'id'>): Observable<Exam> {
    return this.examRepository.create(exam);
  }
}
