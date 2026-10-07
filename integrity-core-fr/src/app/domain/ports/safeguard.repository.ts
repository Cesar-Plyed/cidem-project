import { Observable } from 'rxjs';
import { SafeguardConfig, SafeguardEvent, SafeguardSession } from '../models/safeguard/safeguard.model';

/** Puerto hacia la API de SafeGuard. La implementación HTTP vive en infrastructure/http. */
export abstract class SafeguardRepository {
  abstract getConfig(examId: string): Observable<SafeguardConfig>;
  abstract startSession(examId: string, attemptId: string): Observable<SafeguardSession>;
  abstract registerEvent(event: SafeguardEvent): Observable<void>;
  abstract heartbeat(sessionId: string): Observable<void>;
  abstract closeSession(sessionId: string): Observable<void>;
}
