import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OversightRepository } from '../../../domain/ports/oversight.repository';
import { IntegritySnapshot } from '../../../domain/models/oversight/oversight.model';

/**
 * Caso de uso: snapshots de integridad de los intentos de un examen
 * (GET /api/oversight/exams/{examId}/summary).
 */
@Injectable({ providedIn: 'root' })
export class GetExamSnapshotsUseCase {
  private readonly repository = inject(OversightRepository);

  execute(examId: string): Observable<IntegritySnapshot[]> {
    return this.repository.getExamSnapshots(examId);
  }
}
