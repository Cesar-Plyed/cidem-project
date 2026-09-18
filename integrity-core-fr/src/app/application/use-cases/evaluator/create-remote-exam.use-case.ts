import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { BackendExam } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class CreateRemoteExamUseCase {
  constructor(private readonly evaluatorRepository: EvaluatorRepository) {}

  execute(title: string, description: string | undefined, createdByUserId: string): Observable<BackendExam> {
    return this.evaluatorRepository.createExam(title, description, createdByUserId);
  }
}
