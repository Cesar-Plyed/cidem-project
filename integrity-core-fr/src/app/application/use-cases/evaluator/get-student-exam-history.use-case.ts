import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EvaluatorRepository } from '../../../domain/ports/evaluator.repository';
import { StudentExamHistoryEntry } from '../../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class GetStudentExamHistoryUseCase {
  private readonly evaluatorRepository = inject(EvaluatorRepository);

  execute(): Observable<StudentExamHistoryEntry[]> {
    return this.evaluatorRepository.getStudentExamHistory();
  }
}
