import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendExamAttempt } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class JoinRemoteExamUseCase {
  constructor(private readonly evaluatorRepository: EvaluatorRepository) {}

  execute(examId: string, code: string, studentId: string): Observable<BackendExamAttempt> {
    return this.evaluatorRepository.joinExam(examId, code, studentId);
  }
}
