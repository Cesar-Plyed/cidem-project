import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendExamAttempt } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class SubmitRemoteAttemptUseCase {
  constructor(private readonly evaluatorRepository: EvaluatorRepository) {}

  execute(attemptId: string): Observable<BackendExamAttempt> {
    return this.evaluatorRepository.submitAttempt(attemptId);
  }
}
