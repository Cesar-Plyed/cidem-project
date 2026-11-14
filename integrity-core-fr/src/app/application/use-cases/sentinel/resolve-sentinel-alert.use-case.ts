import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SentinelRepository } from '../../../domain/ports/sentinel.repository';

/** Caso de uso: marcar una alerta como resuelta. */
@Injectable({ providedIn: 'root' })
export class ResolveSentinelAlertUseCase {
  private readonly repository = inject(SentinelRepository);

  execute(alertId: string): Observable<void> {
    return this.repository.resolveAlert(alertId);
  }
}
