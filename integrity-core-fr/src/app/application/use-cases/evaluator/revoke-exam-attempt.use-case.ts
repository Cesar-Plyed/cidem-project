import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendExamAttempt } from '../../../domain/models/evaluator/evaluator.model';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';

@Injectable({ providedIn: 'root' })
export class RevokeExamAttemptUseCase {
  private readonly evaluatorRepository = inject(EvaluatorRepository);

  execute(attemptId: string): Observable<BackendExamAttempt> {
    return this.evaluatorRepository.revokeAttempt(attemptId);
  }
}
