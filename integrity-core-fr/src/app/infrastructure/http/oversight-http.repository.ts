import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OversightRepository } from '../../domain/ports/oversight.repository';
import { LogEntry, IntegritySnapshot, ExamSummary } from '../../domain/models/oversight/oversight.model';

interface BackendLogEntryResponse {
  logId: string;
  examId: string;
  attemptId: string;
  studentId: string;
  studentName: string;
  source: string;
  eventType: string;
  severity: string;
  message: string;
  occurredAt: string;
  receivedAt: string;
}

interface BackendIntegritySnapshotResponse {
  attemptId: string;
  examId: string;
  studentId: string;
  studentName: string;
  integrityIndex: number;
  riskLevel: string;
  totalAlerts: number;
  totalBlocks: number;
  lastEventAt: string | null;
  updatedAt: string;
}

interface BackendExamSummaryResponse {
  examId: string;
  examTitle: string;
  totalAttempts: number;
  activeAttempts: number;
  completedAttempts: number;
  averageIntegrityIndex: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
}

/**
 * Adaptador contra el módulo Oversight real del backend.
 *
 * Contrato backend: /api/oversight
 * - GET /exams/{examId}/summary
 * - GET /exams/{examId}/timeline
 * - GET /attempts/{attemptId}/integrity
 */
@Injectable({ providedIn: 'root' })
export class OversightHttpRepository extends OversightRepository {
  private readonly baseUrl = `${environment.apiUrl}/api/oversight`;
  private readonly http = inject(HttpClient);

  getExamSummary(examId: string): Observable<ExamSummary[]> {
    return this.http
      .get<BackendExamSummaryResponse[]>(`${this.baseUrl}/exams/${examId}/summary`)
      .pipe(map((items) => items.map((raw) => mapExamSummary(raw))));
  }

  getExamTimeline(examId: string): Observable<LogEntry[]> {
    return this.http
      .get<BackendLogEntryResponse[]>(`${this.baseUrl}/exams/${examId}/timeline`)
      .pipe(map((items) => items.map((raw) => mapLogEntry(raw))));
  }

  getAttemptIntegrity(attemptId: string): Observable<IntegritySnapshot | null> {
    return this.http
      .get<BackendIntegritySnapshotResponse>(`${this.baseUrl}/attempts/${attemptId}/integrity`)
      .pipe(
        map((raw) => mapIntegritySnapshot(raw)),
        // Si el intento no existe, el backend devuelve 404
        // El interceptor HTTP ya maneja los errores, pero por si acaso
      );
  }
}

function mapLogEntry(raw: BackendLogEntryResponse): LogEntry {
  return {
    logId: raw.logId,
    examId: raw.examId,
    attemptId: raw.attemptId,
    studentId: raw.studentId,
    studentName: raw.studentName,
    source: raw.source as LogEntry['source'],
    eventType: raw.eventType,
    severity: raw.severity as LogEntry['severity'],
    message: raw.message,
    occurredAt: raw.occurredAt,
    receivedAt: raw.receivedAt,
  };
}

function mapIntegritySnapshot(raw: BackendIntegritySnapshotResponse): IntegritySnapshot {
  return {
    attemptId: raw.attemptId,
    examId: raw.examId,
    studentId: raw.studentId,
    studentName: raw.studentName,
    integrityIndex: raw.integrityIndex,
    riskLevel: raw.riskLevel as IntegritySnapshot['riskLevel'],
    totalAlerts: raw.totalAlerts,
    totalBlocks: raw.totalBlocks,
    lastEventAt: raw.lastEventAt,
    updatedAt: raw.updatedAt,
  };
}

function mapExamSummary(raw: BackendExamSummaryResponse): ExamSummary {
  return {
    examId: raw.examId,
    examTitle: raw.examTitle,
    totalAttempts: raw.totalAttempts,
    activeAttempts: raw.activeAttempts,
    completedAttempts: raw.completedAttempts,
    averageIntegrityIndex: raw.averageIntegrityIndex,
    highRiskCount: raw.highRiskCount,
    mediumRiskCount: raw.mediumRiskCount,
    lowRiskCount: raw.lowRiskCount,
  };
}