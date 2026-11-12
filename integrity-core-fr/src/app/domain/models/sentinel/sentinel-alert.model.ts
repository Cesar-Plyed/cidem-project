/** Espejo de `AlertResponseDTO` del backend Sentinel. */
export type SentinelSeverity = 'LOW' | 'MEDIUM' | 'HIGH';

export interface SentinelAlert {
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

export interface SentinelAlertFilters {
  severity?: SentinelSeverity;
  from?: string;
  to?: string;
}

export const SentinelSeverityConfig: Record<
  SentinelSeverity,
  { label: string; color: string }
> = {
  LOW: { label: 'Baja', color: 'success' },
  MEDIUM: { label: 'Media', color: 'warning' },
  HIGH: { label: 'Alta', color: 'danger' },
};
