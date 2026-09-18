import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendExamAccessCode } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class GenerateAccessCodeUseCase {
  constructor(private readonly evaluatorRepository: EvaluatorRepository) {}

  execute(examId: string, expiresInMinutes = 20, code?: string): Observable<BackendExamAccessCode> {
    return this.evaluatorRepository.generateAccessCode(examId, expiresInMinutes, code);
  }
}
