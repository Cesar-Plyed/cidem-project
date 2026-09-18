import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExamRepository } from '../../../domain/ports/exam.repository';
import { Exam } from '../../../domain/models/exam/exam.model';

@Injectable({ providedIn: 'root' })
export class UpdateExamUseCase {
  constructor(private readonly examRepository: ExamRepository) {}

  execute(exam: Exam): Observable<Exam> {
    return this.examRepository.update(exam);
  }
}
