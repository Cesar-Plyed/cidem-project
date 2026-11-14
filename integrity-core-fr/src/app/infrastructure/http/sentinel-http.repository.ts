import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SentinelRepository } from '../../domain/ports/sentinel.repository';
import {
  SentinelAlert,
  SentinelAlertFilters,
  SentinelSeverity,
} from '../../domain/models/sentinel/sentinel-alert.model';

/** Contrato real de `AlertResponseDTO` (backend Sentinel). */
interface BackendAlertResponse {
  id: string;
  examId: string | null;
  attemptId: string;
  examSessionId: string;
  studentId: string;
  studentName: string | null;
  examName: string | null;
  eventType: string;
  severity: SentinelSeverity;
  reason: string;
  occurredAt: string;
  resolved: boolean;
}

@Injectable({ providedIn: 'root' })
export class SentinelHttpRepository extends SentinelRepository {
  private readonly baseUrl = `${environment.apiUrl}/sentinel`;
  private readonly http = inject(HttpClient);

  getExamAlerts(
    examId: string,
    filters: SentinelAlertFilters
  ): Observable<SentinelAlert[]> {
    let params = new HttpParams();
    if (filters.severity) {
      params = params.set('severity', filters.severity);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }

    return this.http
      .get<BackendAlertResponse[]>(`${this.baseUrl}/exams/${examId}/alerts`, { params })
      .pipe(map((items) => items.map((raw) => this.toModel(raw))));
  }

  resolveAlert(alertId: string): Observable<void> {
    return this.http
      .put<void>(`${this.baseUrl}/alerts/${alertId}/resolve`, null)
      .pipe(map(() => undefined));
  }

  private toModel(raw: BackendAlertResponse): SentinelAlert {
    return {
      id: raw.id,
      examId: raw.examId,
      attemptId: raw.attemptId,
      examSessionId: raw.examSessionId,
      studentId: raw.studentId,
      studentName: raw.studentName,
      examName: raw.examName,
      eventType: raw.eventType,
      severity: raw.severity,
      reason: raw.reason,
      occurredAt: raw.occurredAt,
      resolved: raw.resolved,
    };
  }
}
