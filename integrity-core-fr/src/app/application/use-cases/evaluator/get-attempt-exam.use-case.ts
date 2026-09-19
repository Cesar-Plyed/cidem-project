import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { AttemptExam } from '../../../domain/models/evaluator/attempt-exam.model';

@Injectable({ providedIn: 'root' })
export class GetAttemptExamUseCase {
  private readonly evaluatorRepository = inject(EvaluatorRepository);

  execute(attemptId: string): Observable<AttemptExam> {
    return this.evaluatorRepository.getAttemptExam(attemptId);
  }
}
