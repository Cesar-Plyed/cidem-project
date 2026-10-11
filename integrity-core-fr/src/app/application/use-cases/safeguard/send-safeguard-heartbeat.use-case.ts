import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SafeguardRepository } from '../../../domain/ports/safeguard.repository';

@Injectable({ providedIn: 'root' })
export class SendSafeguardHeartbeatUseCase {
  private readonly safeguardRepository = inject(SafeguardRepository);

  execute(sessionId: string): Observable<void> {
    return this.safeguardRepository.heartbeat(sessionId);
  }
}
