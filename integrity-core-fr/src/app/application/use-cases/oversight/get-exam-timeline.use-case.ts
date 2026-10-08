import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OversightRepository } from '../../../domain/ports/oversight.repository';
import { LogEntry } from '../../../domain/models/oversight/oversight.model';

/** Caso de uso: Obtener línea de tiempo (eventos) de un examen. */
@Injectable({ providedIn: 'root' })
export class GetExamTimelineUseCase {
  private readonly repository = inject(OversightRepository);

  execute(examId: string): Observable<LogEntry[]> {
    return this.repository.getExamTimeline(examId);
  }
}