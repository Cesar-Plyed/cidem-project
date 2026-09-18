import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendExamAnswer } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class SubmitRemoteAnswerUseCase {
  constructor(private readonly evaluatorRepository: EvaluatorRepository) {}

  execute(
    attemptId: string,
    questionId: string,
    selectedOptionIds: string[] = [],
    openTextAnswer?: string,
  ): Observable<BackendExamAnswer> {
    return this.evaluatorRepository.submitAnswer(attemptId, questionId, selectedOptionIds, openTextAnswer);
  }
}
