import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OversightRepository } from '../../../domain/ports/oversight.repository';
import { ExamSummary } from '../../../domain/models/oversight/oversight.model';

/** Caso de uso: Obtener resumen de integridad de un examen. */
@Injectable({ providedIn: 'root' })
export class GetExamSummaryUseCase {
  private readonly repository = inject(OversightRepository);

  execute(examId: string): Observable<ExamSummary[]> {
    return this.repository.getExamSummary(examId);
  }
}