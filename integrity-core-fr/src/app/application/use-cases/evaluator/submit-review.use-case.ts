import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendEvaluationReview } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class SubmitReviewUseCase {
  private readonly evaluatorRepository = inject(EvaluatorRepository);

  execute(
    answerId: string,
    score: number,
    feedback?: string,
  ): Observable<BackendEvaluationReview> {
    return this.evaluatorRepository.reviewOpenEndedAnswer(answerId, score, feedback);
  }
}
