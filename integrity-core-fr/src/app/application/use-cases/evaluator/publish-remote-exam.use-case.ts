import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendExam } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class PublishRemoteExamUseCase {
  private readonly evaluatorRepository = inject(EvaluatorRepository);

  execute(examId: string): Observable<BackendExam> {
    return this.evaluatorRepository.publishExam(examId);
  }
}
