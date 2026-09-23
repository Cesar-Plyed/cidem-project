import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendOpenEndedReview } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class ListOpenEndedReviewsUseCase {
  private readonly evaluatorRepository = inject(EvaluatorRepository);

  execute(examId: string, onlyPending = true): Observable<BackendOpenEndedReview[]> {
    return this.evaluatorRepository.listOpenEndedReviews(examId, onlyPending);
  }
}
