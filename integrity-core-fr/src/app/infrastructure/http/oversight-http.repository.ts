import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OversightRepository } from '../../domain/ports/oversight.repository';
import { LogEntry, IntegritySnapshot, RiskLevel } from '../../domain/models/oversight/oversight.model';

/** Contrato real de `LogEntryResponse`. */
interface BackendLogEntryResponse {
  studentId: string;
  attemptId: string;
  source: 'SENTINEL' | 'SAFEGUARD' | 'EVALUATOR';
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  description: string;
  occurredAt: string;
}

/** Contrato real de `IntegritySnapshotResponse` (summary e integrity). */
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

/**
 * Adaptador contra el módulo Oversight real del backend.
 *
 * Contrato backend:
 * - GET /api/oversight/exams/{examId}/summary  → IntegritySnapshotResponse[]
 * - GET /api/oversight/exams/{examId}/timeline → LogEntryResponse[]
 * - GET /api/oversight/attempts/{attemptId}/integrity → IntegritySnapshotResponse
 *
 * Oversight no resuelve nombres de alumno: la página los une con los
 * resultados del examen (evaluator, staff-only).
 */
@Injectable({ providedIn: 'root' })
export class OversightHttpRepository extends OversightRepository {
  private readonly baseUrl = `${environment.apiUrl}/api/oversight`;
  private readonly http = inject(HttpClient);

  getExamSnapshots(examId: string): Observable<IntegritySnapshot[]> {
    return this.http
      .get<BackendIntegritySnapshotResponse[]>(`${this.baseUrl}/exams/${examId}/summary`)
      .pipe(map((items) => items.map((raw) => mapIntegritySnapshot(raw))));
  }

  getExamTimeline(examId: string): Observable<LogEntry[]> {
    return this.http
      .get<BackendLogEntryResponse[]>(`${this.baseUrl}/exams/${examId}/timeline`)
      .pipe(map((items) => items.map((raw) => mapLogEntry(raw))));
  }

  getAttemptIntegrity(attemptId: string): Observable<IntegritySnapshot | null> {
    // Si el intento no existe el backend devuelve 404 y el interceptor HTTP
    // se encarga del error.
    return this.http
      .get<BackendIntegritySnapshotResponse>(`${this.baseUrl}/attempts/${attemptId}/integrity`)
      .pipe(map((raw) => mapIntegritySnapshot(raw)));
  }
}

function mapLogEntry(raw: BackendLogEntryResponse): LogEntry {
  return {
    attemptId: raw.attemptId,
    studentId: raw.studentId,
    source: raw.source,
    severity: raw.severity,
    message: raw.description,
    occurredAt: raw.occurredAt,
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
