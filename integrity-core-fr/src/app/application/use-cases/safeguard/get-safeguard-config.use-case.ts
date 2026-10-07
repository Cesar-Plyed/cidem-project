import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SafeguardRepository } from '../../../domain/ports/safeguard.repository';
import { SafeguardConfig } from '../../../domain/models/safeguard/safeguard.model';

@Injectable({ providedIn: 'root' })
export class GetSafeguardConfigUseCase {
  private readonly safeguardRepository = inject(SafeguardRepository);

  execute(examId: string): Observable<SafeguardConfig> {
    return this.safeguardRepository.getConfig(examId);
  }
}
