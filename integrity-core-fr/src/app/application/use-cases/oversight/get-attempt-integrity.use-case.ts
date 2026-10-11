import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OversightRepository } from '../../../domain/ports/oversight.repository';
import { IntegritySnapshot } from '../../../domain/models/oversight/oversight.model';

/** Caso de uso: Obtener índice de integridad de un intento. */
@Injectable({ providedIn: 'root' })
export class GetAttemptIntegrityUseCase {
  private readonly repository = inject(OversightRepository);

  execute(attemptId: string): Observable<IntegritySnapshot | null> {
    return this.repository.getAttemptIntegrity(attemptId);
  }
}