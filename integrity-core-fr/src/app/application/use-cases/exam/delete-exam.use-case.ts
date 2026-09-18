import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExamRepository } from '../../../domain/ports/exam.repository';

@Injectable({ providedIn: 'root' })
export class DeleteExamUseCase {
  constructor(private readonly examRepository: ExamRepository) {}

  execute(id: string): Observable<void> {
    return this.examRepository.delete(id);
  }
}
