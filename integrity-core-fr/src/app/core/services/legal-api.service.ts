import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BreachNotice,
  ConsentHistory,
  ConsentOverview,
  ConsentType,
  DataTypesInfo,
  DpiaList,
  DpiaStatus,
  RestrictionStatus,
  RestrictionType,
  RetentionInfo,
} from '../../domain/models/legal/legal.model';

/** Cliente HTTP del modulo legal del backend (specs 36-50). */
@Injectable({ providedIn: 'root' })
export class LegalApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/legal`;

  getConsentStatus(studentId: string): Observable<ConsentOverview> {
    return this.http.get<ConsentOverview>(`${this.baseUrl}/consent/${studentId}`);
  }

  grantConsent(studentId: string, consentType: ConsentType): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/consent/grant`, { studentId, consentType });
  }

  revokeConsent(studentId: string, consentType: ConsentType): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/consent/revoke`, { studentId, consentType });
  }

  revokeAllConsents(studentId: string): Observable<unknown[]> {
    return this.http.post<unknown[]>(`${this.baseUrl}/consent/revoke-all`, { studentId });
  }

  getConsentHistory(studentId: string): Observable<ConsentHistory> {
    return this.http.get<ConsentHistory>(`${this.baseUrl}/consent/history`, {
      params: { studentId },
    });
  }

  getRetention(): Observable<RetentionInfo> {
    return this.http.get<RetentionInfo>(`${this.baseUrl}/data-retention`);
  }

  getDataTypes(): Observable<DataTypesInfo> {
    return this.http.get<DataTypesInfo>(`${this.baseUrl}/data-types`);
  }

  getAccess(studentId: string): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.baseUrl}/access/${studentId}`);
  }

  exportAccess(studentId: string): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/access/${studentId}/export`, {}, { responseType: 'blob' });
  }

  exportPortability(studentId: string, format: 'json' | 'csv'): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/portability/${studentId}/${format}`, {
      responseType: 'blob',
    });
  }

  rectifyData(entityType: string, entityId: string, fields: Record<string, unknown>): Observable<unknown> {
    return this.http.put(`${this.baseUrl}/data/${entityType}/${entityId}`, fields);
  }

  softDeleteData(entityType: string, entityId: string): Observable<unknown> {
    return this.http.delete(`${this.baseUrl}/data/${entityType}/${entityId}`);
  }

  eraseStudentData(studentId: string, mode: 'full' | 'soft'): Observable<unknown> {
    return this.http.delete(`${this.baseUrl}/data/${studentId}/${mode}`);
  }

  submitRestriction(studentId: string, restrictionType: RestrictionType, reason: string): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/restrict`, { studentId, restrictionType, reason });
  }

  revokeRestriction(restrictionId: string): Observable<unknown> {
    return this.http.delete(`${this.baseUrl}/restrict/${restrictionId}`);
  }

  getRestrictionStatus(studentId: string): Observable<RestrictionStatus> {
    return this.http.get<RestrictionStatus>(`${this.baseUrl}/restrict/${studentId}/status`);
  }

  submitObject(studentId: string, objectType: string, reason: string): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/object`, { studentId, objectType, reason });
  }

  getPendingBreaches(studentId: string): Observable<{ notifications: BreachNotice[] }> {
    return this.http.get<{ notifications: BreachNotice[] }>(
      `${this.baseUrl}/breach/${studentId}/pending`,
    );
  }

  resolveBreach(notificationId: string): Observable<unknown> {
    return this.http.put(`${this.baseUrl}/breach/${notificationId}/resolve`, { status: 'RESOLVED' });
  }

  getPendingDpias(studentId: string): Observable<DpiaList> {
    return this.http.get<DpiaList>(`${this.baseUrl}/dpia/${studentId}/pending`);
  }

  reviewDpia(dpiId: string, status: DpiaStatus): Observable<unknown> {
    return this.http.put(`${this.baseUrl}/dpia/${dpiId}/review`, { status });
  }

  downloadDpiaPdf(dpiId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/dpia/${dpiId}/pdf`, { responseType: 'blob' });
  }
}
