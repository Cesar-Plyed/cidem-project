import { Observable } from 'rxjs';
import { LogEntry, IntegritySnapshot } from '../models/oversight/oversight.model';

/** Puerto hacia la API de Oversight. La implementación HTTP vive en infrastructure/http. */
export abstract class OversightRepository {
  abstract getExamSnapshots(examId: string): Observable<IntegritySnapshot[]>;
  abstract getExamTimeline(examId: string): Observable<LogEntry[]>;
  abstract getAttemptIntegrity(attemptId: string): Observable<IntegritySnapshot | null>;
}
