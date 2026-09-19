import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExamRepository } from '../../../domain/ports/exam.repository';

@Injectable({ providedIn: 'root' })
export class DeleteExamUseCase {
  private readonly examRepository = inject(ExamRepository)

  execute(id: string): Observable<void> {
    return this.examRepository.delete(id);
  }
}
