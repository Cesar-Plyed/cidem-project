import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SafeguardRepository } from '../../../domain/ports/safeguard.repository';
import { SafeguardEvent } from '../../../domain/models/safeguard/safeguard.model';

@Injectable({ providedIn: 'root' })
export class RegisterSafeguardEventUseCase {
  private readonly safeguardRepository = inject(SafeguardRepository);

  execute(event: SafeguardEvent): Observable<void> {
    return this.safeguardRepository.registerEvent(event);
  }
}
