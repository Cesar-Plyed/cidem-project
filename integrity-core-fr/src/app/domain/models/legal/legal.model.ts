/** Contrato del modulo legal (specs 36-50). */

export type ConsentType = 'CAMERA' | 'MICROPHONE' | 'SCREENSHOT' | 'TAB_MONITORING';
export type ConsentState = 'granted' | 'revoked';
export type DpiaStatus = 'DRAFT' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type BreachType = 'DATA_LEAK' | 'UNAUTHORIZED_ACCESS' | 'SYSTEM_COMPROMISE';
export type RestrictionType = 'PROCESSING' | 'STORAGE' | 'SHARING';

export const CONSENT_TYPES: ConsentType[] = ['CAMERA', 'MICROPHONE', 'SCREENSHOT', 'TAB_MONITORING'];

export interface ConsentStatus {
  id: string | null;
  type: ConsentType;
  status: ConsentState;
  grantedAt: string | null;
  revokedAt: string | null;
}

export interface ConsentOverview {
  studentId: string;
  consents: ConsentStatus[];
}

export interface ConsentHistoryEntry {
  type: ConsentType;
  action: ConsentState;
  timestamp: string;
}

export interface ConsentHistory {
  studentId: string;
  history: ConsentHistoryEntry[];
}

export interface RetentionInfo {
  retentionYears: number;
  dataTypes: string[];
  nextDeletionDate: string;
}

export interface DataTypesInfo {
  required: string[];
  optional: string[];
  retentionDays: Record<string, number>;
}

export interface BreachNotice {
  id: string;
  breachType: BreachType;
  description: string;
  recommendedAction: string;
  notifiedAt: string;
  status: 'PENDING' | 'NOTIFIED' | 'RESOLVED';
}

export interface RestrictionStatus {
  restrictionId: string | null;
  restricted: boolean;
  restrictionType: RestrictionType | null;
  reason: string | null;
  requestedAt: string | null;
  expiresAt: string | null;
  status: string | null;
}

export interface DpiaSummary {
  id: string;
  title: string;
  riskLevel: RiskLevel;
  status: DpiaStatus;
}

export interface DpiaList {
  dpias: DpiaSummary[];
}

export interface LegalNoticeContent {
  title: string;
  responsible: string;
  purpose: string;
  legalBasis: string;
  rights: string[];
  contact: string;
  dpo: string;
  articles: string[];
  updatedAt: string;
}

export interface ConsentFormItem {
  type: ConsentType;
  label: string;
  description: string;
  required: boolean;
}

export interface ExportArtifact {
  url: string;
  size: number;
  generatedAt: string;
  format: string;
}
