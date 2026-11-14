import { Observable } from 'rxjs';
import { SentinelAlert, SentinelAlertFilters } from '../models/sentinel/sentinel-alert.model';

/**
 * Puerto hacia la API de Sentinel. La implementación HTTP vive en
 * infrastructure/http y se conecta en main.ts.
 */
export abstract class SentinelRepository {
  abstract getExamAlerts(
    examId: string,
    filters: SentinelAlertFilters
  ): Observable<SentinelAlert[]>;

  abstract resolveAlert(alertId: string): Observable<void>;
}
