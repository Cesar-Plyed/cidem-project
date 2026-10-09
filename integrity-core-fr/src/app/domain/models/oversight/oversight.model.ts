export interface LogEntry {
  logId: string;
  examId: string;
  attemptId: string;
  studentId: string;
  studentName: string;
  source: LogSource;
  eventType: string;
  severity: Severity;
  message: string;
  occurredAt: string;
  receivedAt: string;
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

export interface ExamSummary {
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

export type LogSource = 'SAFEGUARD' | 'SENTINEL' | 'EVALUATOR' | 'SYSTEM';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
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
  CRITICAL: { label: 'Crítico', color: 'dark' },
};

export const LogSourceConfig: Record<LogSource, { label: string; icon: string }> = {
  SAFEGUARD: { label: 'SafeGuard', icon: 'lock-closed' },
  SENTINEL: { label: 'Sentinel', icon: 'warning' },
  EVALUATOR: { label: 'Evaluator', icon: 'document-text' },
  SYSTEM: { label: 'Sistema', icon: 'cog' },
};