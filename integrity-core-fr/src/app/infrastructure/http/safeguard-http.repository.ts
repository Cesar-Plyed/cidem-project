import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SafeguardRepository } from '../../domain/ports/safeguard.repository';
import {
  DEFAULT_SAFEGUARD_CONFIG,
  SafeguardConfig,
  SafeguardEvent,
  SafeguardSession,
} from '../../domain/models/safeguard/safeguard.model';

interface BackendSafeguardConfigResponse {
  examId: string;
  enabled: boolean;
  requireCamera: boolean;
  requireMicrophone: boolean;
  requireFullscreen: boolean;
  blockCopyPaste: boolean;
  maxTabSwitches: number;
  riskThreshold: number;
  autoTerminateOnThreshold: boolean;
}

interface BackendSessionResponse {
  sessionId: string;
  examId: string;
  attemptId: string;
  status: SafeguardSession['status'];
  riskScore: number;
  startedAt: string;
  endedAt?: string | null;
  lastHeartbeatAt?: string | null;
  requireCamera: boolean;
  requireMicrophone: boolean;
  requireFullscreen: boolean;
  blockCopyPaste: boolean;
}

interface BackendRecordEventRequest {
  sessionId: string;
  eventId: string;
  eventType: BackendSafeguardEventType;
  occurredAt: string;
  metadata?: string;
}

interface BackendHeartbeatRequest {
  clientTime: string;
}

type BackendSafeguardEventType =
  | 'TAB_SWITCH'
  | 'WINDOW_BLUR'
  | 'FULLSCREEN_EXIT'
  | 'COPY_PASTE'
  | 'DEVTOOLS_OPENED'
  | 'RIGHT_CLICK'
  | 'SUSPICIOUS_ACTIVITY';

/**
 * Adaptador contra el módulo SafeGuard real del backend.
 *
 * Contrato backend: /api/safeguard
 * - GET  /exams/{examId}/config
 * - POST /sessions/start
 * - POST /events
 * - POST /sessions/{sessionId}/heartbeat
 * - POST /sessions/{sessionId}/close
 *
 * El alumno no se envía en ningún request: el backend lo obtiene del JWT.
 * Los tipos locales del monitor se traducen aquí al enum del backend.
 */
@Injectable({ providedIn: 'root' })
export class SafeguardHttpRepository extends SafeguardRepository {
  private readonly baseUrl = `${environment.apiUrl}/api/safeguard`;
  private readonly http = inject(HttpClient);

  getConfig(examId: string): Observable<SafeguardConfig> {
    return this.http
      .get<BackendSafeguardConfigResponse>(`${this.baseUrl}/exams/${examId}/config`)
      .pipe(map((raw) => normalizeConfig(raw)));
  }

  startSession(examId: string, attemptId: string): Observable<SafeguardSession> {
    const userAgent = typeof navigator !== 'undefined' ? navigator.userAgent : undefined;

    return this.http
      .post<BackendSessionResponse>(`${this.baseUrl}/sessions/start`, {
        examId,
        attemptId,
        userAgent,
      })
      .pipe(map((raw) => mapSession(raw)));
  }

  registerEvent(event: SafeguardEvent): Observable<void> {
    const request: BackendRecordEventRequest = {
      sessionId: event.sessionId,
      eventId: event.eventId,
      eventType: mapEventType(event.type),
      occurredAt: event.occurredAt,
      metadata: buildMetadata(event),
    };

    return this.http.post(`${this.baseUrl}/events`, request).pipe(map(() => undefined));
  }

  heartbeat(sessionId: string): Observable<void> {
    const body: BackendHeartbeatRequest = { clientTime: new Date().toISOString() };
    return this.http
      .post(`${this.baseUrl}/sessions/${sessionId}/heartbeat`, body)
      .pipe(map(() => undefined));
  }

  closeSession(sessionId: string): Observable<void> {
    return this.http.post(`${this.baseUrl}/sessions/${sessionId}/close`, null).pipe(map(() => undefined));
  }
}

/** Convierte la respuesta real del backend a la configuración que usa el monitor web. */
export function normalizeConfig(
  raw: Partial<BackendSafeguardConfigResponse> | null | undefined,
): SafeguardConfig {
  const base = DEFAULT_SAFEGUARD_CONFIG;
  const source = raw ?? {};
  const flag = (value: unknown, fallback: boolean): boolean =>
    typeof value === 'boolean' ? value : fallback;
  const nonNegativeInt = (value: unknown, fallback: number): number =>
    typeof value === 'number' && Number.isInteger(value) && value >= 0 ? value : fallback;
  const positiveInt = (value: unknown, fallback: number): number =>
    typeof value === 'number' && Number.isInteger(value) && value > 0 ? value : fallback;

  const blockCopyPaste = flag(source.blockCopyPaste, base.blockCopyPaste);

  return {
    enabled: flag(source.enabled, base.enabled),
    requireCamera: flag(source.requireCamera, base.requireCamera),
    requireMicrophone: flag(source.requireMicrophone, base.requireMicrophone),
    requireFullscreen: flag(source.requireFullscreen, base.requireFullscreen),
    blockCopyPaste,
    maxTabSwitches: nonNegativeInt(source.maxTabSwitches, base.maxTabSwitches),
    riskThreshold: positiveInt(source.riskThreshold, base.riskThreshold),
    autoTerminateOnThreshold: flag(source.autoTerminateOnThreshold, base.autoTerminateOnThreshold),

    // El backend actual no separa copy/cut/paste ni expone flags de menú/atajos.
    // blockCopyPaste se replica a las tres acciones; el resto conserva la política web.
    detectTabSwitch: base.detectTabSwitch,
    detectFocusLoss: base.detectFocusLoss,
    blockCopy: blockCopyPaste,
    blockCut: blockCopyPaste,
    blockPaste: blockCopyPaste,
    blockContextMenu: base.blockContextMenu,
    blockRestrictedShortcuts: base.blockRestrictedShortcuts,
    restrictedShortcuts: [...base.restrictedShortcuts],
    eventThrottleMs: base.eventThrottleMs,
    heartbeatIntervalSeconds: base.heartbeatIntervalSeconds,
    detectDevtools: base.detectDevtools,
    blockPrint: base.blockPrint,
  };
}

export function mapEventType(type: SafeguardEvent['type']): BackendSafeguardEventType {
  switch (type) {
    case 'TAB_HIDDEN':
      return 'TAB_SWITCH';
    case 'WINDOW_BLUR':
      return 'WINDOW_BLUR';
    case 'FULLSCREEN_EXIT':
      return 'FULLSCREEN_EXIT';
    case 'COPY_ATTEMPT':
    case 'CUT_ATTEMPT':
    case 'PASTE_ATTEMPT':
      return 'COPY_PASTE';
    case 'CONTEXT_MENU':
      return 'RIGHT_CLICK';
    case 'RESTRICTED_SHORTCUT':
      return 'SUSPICIOUS_ACTIVITY';
    case 'DEVTOOLS_OPENED':
      return 'DEVTOOLS_OPENED';
    case 'PRINT_ATTEMPT':
      return 'SUSPICIOUS_ACTIVITY';
    case 'NATIVE_WINDOW_SWITCH':
      return 'TAB_SWITCH';
  }
}

function buildMetadata(event: SafeguardEvent): string {
  const metadata = {
    clientEventType: event.type,
    result: event.result,
    detail: event.detail ?? null,
    examId: event.examId,
    attemptId: event.attemptId,
  };

  return JSON.stringify(metadata);
}

function mapSession(raw: BackendSessionResponse): SafeguardSession {
  return {
    sessionId: raw.sessionId,
    examId: raw.examId,
    attemptId: raw.attemptId,
    status: raw.status,
    riskScore: raw.riskScore,
    startedAt: raw.startedAt,
    endedAt: raw.endedAt ?? null,
    lastHeartbeatAt: raw.lastHeartbeatAt ?? null,
    requireCamera: raw.requireCamera,
    requireMicrophone: raw.requireMicrophone,
    requireFullscreen: raw.requireFullscreen,
    blockCopyPaste: raw.blockCopyPaste,
  };
}
