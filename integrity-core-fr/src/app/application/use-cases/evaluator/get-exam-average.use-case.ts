import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendExamAverage } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class GetExamAverageUseCase {
  private readonly evaluatorRepository = inject(EvaluatorRepository);

  execute(examId: string): Observable<BackendExamAverage> {
    return this.evaluatorRepository.getExamAverage(examId);
  }
}
