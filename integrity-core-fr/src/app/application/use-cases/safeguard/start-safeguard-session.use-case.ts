import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SafeguardRepository } from '../../../domain/ports/safeguard.repository';
import { SafeguardSession } from '../../../domain/models/safeguard/safeguard.model';

@Injectable({ providedIn: 'root' })
export class StartSafeguardSessionUseCase {
  private readonly safeguardRepository = inject(SafeguardRepository);

  execute(examId: string, attemptId: string): Observable<SafeguardSession> {
    return this.safeguardRepository.startSession(examId, attemptId);
  }
}
