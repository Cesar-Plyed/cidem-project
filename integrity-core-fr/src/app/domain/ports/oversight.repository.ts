import { Observable } from 'rxjs';
import { LogEntry, IntegritySnapshot, ExamSummary } from '../models/oversight/oversight.model';

/** Puerto hacia la API de Oversight. La implementación HTTP vive en infrastructure/http. */
export abstract class OversightRepository {
  abstract getExamSummary(examId: string): Observable<ExamSummary[]>;
  abstract getExamTimeline(examId: string): Observable<LogEntry[]>;
  abstract getAttemptIntegrity(attemptId: string): Observable<IntegritySnapshot | null>;
}