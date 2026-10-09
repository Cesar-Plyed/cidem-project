import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OversightRepository } from '../../domain/ports/oversight.repository';
import { LogEntry, IntegritySnapshot, ExamSummary, RiskLevel } from '../../domain/models/oversight/oversight.model';

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

/** Contrato real de GET /api/oversight/attempts/{attemptId}/integrity. */
interface BackendIntegritySnapshotResponse {
  studentId: string;
  attemptId: string;
  score: number;
  /** RiskLevel del dominio del backend, distinto al modelo de la vista. */
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  lastUpdatedAt: string;
}

/** LOW/MEDIUM/HIGH/CRITICAL → GREEN/YELLOW/RED (modelo de la vista). */
const RISK_LEVEL_FROM_BACKEND: Record<
  BackendIntegritySnapshotResponse['riskLevel'],
  RiskLevel
> = {
  LOW: 'GREEN',
  MEDIUM: 'YELLOW',
  HIGH: 'RED',
  CRITICAL: 'RED',
};

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
    studentId: raw.studentId,
    integrityIndex: raw.score,
    riskLevel: RISK_LEVEL_FROM_BACKEND[raw.riskLevel] ?? 'RED',
    updatedAt: raw.lastUpdatedAt,
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