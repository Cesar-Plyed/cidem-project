import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SentinelRepository } from '../../../domain/ports/sentinel.repository';
import {
  SentinelAlert,
  SentinelAlertFilters,
} from '../../../domain/models/sentinel/sentinel-alert.model';

/** Caso de uso: listar las alertas de un examen con filtros. */
@Injectable({ providedIn: 'root' })
export class GetSentinelAlertsUseCase {
  private readonly repository = inject(SentinelRepository);

  execute(examId: string, filters: SentinelAlertFilters = {}): Observable<SentinelAlert[]> {
    return this.repository.getExamAlerts(examId, filters);
  }
}
