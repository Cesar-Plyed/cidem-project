/**
 * Espejo de `LogEntryResponse` del backend (oversight). El backend no envía
 * `logId`, `examId`, `eventType` ni `receivedAt`: `studentName` lo completa la
 * página con los resultados del examen (endpoint staff-only de evaluator).
 */
export interface LogEntry {
  attemptId: string;
  studentId: string;
  studentName?: string;
  source: LogSource;
  severity: Severity;
  message: string;
  occurredAt: string;
}

/**
 * Espejo de `IntegritySnapshotResponse` del backend (oversight). El backend
 * devuelve `score` + `riskLevel` (LOW/MEDIUM/HIGH/CRITICAL) y `lastUpdatedAt`;
 * `studentName` lo completa la página con el nombre de la fila que se clicó,
 * porque el snapshot no lo incluye.
 */
export interface IntegritySnapshot {
  attemptId: string;
  studentId: string;
  studentName?: string;
  integrityIndex: number;
  riskLevel: RiskLevel;
  updatedAt: string;
}

/**
 * Resumen de integridad del examen, armado en la página con los snapshots de
 * GET /exams/{examId}/summary (los conteos de intentos vienen del promedio de
 * evaluator, porque oversight no los expone).
 */
export interface ExamIntegritySummary {
  examId: string;
  examTitle: string;
  totalAttempts: number;
  gradedAttempts: number;
  pendingReviewCount: number;
  averageIntegrityIndex: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
}

// Sentinels solo genera severidades LOW/MEDIUM/HIGH y el log solo registra
// orígenes SENTINEL/SAFEGUARD/EVALUATOR (CHECK constraints en V9), así que no
// existen CRITICAL ni SYSTEM.
export type LogSource = 'SAFEGUARD' | 'SENTINEL' | 'EVALUATOR';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH';
export type RiskLevel = 'GREEN' | 'YELLOW' | 'RED';

export const RiskLevelConfig: Record<RiskLevel, { label: string; color: string; icon: string }> = {
  GREEN: { label: 'Bajo riesgo', color: 'success', icon: 'shield-checkmark' },
  YELLOW: { label: 'Riesgo medio', color: 'warning', icon: 'shield-alert' },
  RED: { label: 'Alto riesgo', color: 'danger', icon: 'shield-close' },
};

export const SeverityConfig: Record<Severity, { label: string; color: string }> = {
  LOW: { label: 'Bajo', color: 'success' },
  MEDIUM: { label: 'Medio', color: 'warning' },
  HIGH: { label: 'Alto', color: 'danger' },
};

export const LogSourceConfig: Record<LogSource, { label: string; icon: string }> = {
  SAFEGUARD: { label: 'SafeGuard', icon: 'lock-closed' },
  SENTINEL: { label: 'Sentinel', icon: 'warning' },
  EVALUATOR: { label: 'Evaluator', icon: 'document-text' },
};