import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendExamResult } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class GetExamResultsUseCase {
  private readonly evaluatorRepository = inject(EvaluatorRepository);

  execute(examId: string): Observable<BackendExamResult[]> {
    return this.evaluatorRepository.getExamResults(examId);
  }
}
