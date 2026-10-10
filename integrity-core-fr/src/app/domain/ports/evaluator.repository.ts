import { Observable } from 'rxjs';
import {
  BackendExam,
  BackendExamAccessCode,
  BackendExamAnswer,
  BackendExamAttempt,
  BackendExamAverage,
  BackendExamResult,
  BackendEvaluationReview,
  BackendOpenEndedReview,
} from '../models/evaluator/evaluator.model';
import { AttemptExam } from '../models/evaluator/attempt-exam.model';

/**
 * Puerto para el backend REAL de evaluator (no confundir con
 * domain/ports/exam.repository.ts, que es el mock usado por las vistas de
 * docente/alumno mientras el backend no tenga endpoints de lectura).
 *
 * A propósito, este puerto SOLO declara lo que el backend expone hoy
 * (verificado en el código fuente, no en documentación):
 *   - crear examen (sin preguntas, el backend no lo permite todavía)
 *   - generar código de acceso
 *   - unirse por código (crea el intento)
 *   - guardar una respuesta
 *   - entregar el intento
 *   - listar resultados de un examen
 *   - promedio de un examen
 *   - listar revisiones abiertas
 *   - revisar/actualizar una respuesta abierta
 *   - obtener el resultado de un intento
 */
export abstract class EvaluatorRepository {
  abstract createExam(title: string, description: string | undefined, createdByUserId: string): Observable<BackendExam>;

  abstract publishExam(examId: string): Observable<BackendExam>;

  abstract generateAccessCode(
    examId: string,
    expiresInMinutes: number,
    code?: string,
  ): Observable<BackendExamAccessCode>;

  abstract joinExam(examId: string, code: string, studentId: string): Observable<BackendExamAttempt>;

  abstract joinExamByCode(code: string): Observable<BackendExamAttempt>;

  abstract getAttemptExam(attemptId: string): Observable<AttemptExam>;

  abstract submitAnswer(
    attemptId: string,
    questionId: string,
    selectedOptionIds: string[],
    openTextAnswer?: string,
  ): Observable<BackendExamAnswer>;

  abstract submitAttempt(attemptId: string): Observable<BackendExamAttempt>;

  abstract getExamResults(examId: string): Observable<BackendExamResult[]>;

  abstract getAttemptResult(attemptId: string): Observable<BackendExamResult>;

  abstract getExamAverage(examId: string): Observable<BackendExamAverage>;

  abstract listOpenEndedReviews(
    examId: string,
    onlyPending?: boolean,
  ): Observable<BackendOpenEndedReview[]>;

  abstract reviewOpenEndedAnswer(
    answerId: string,
    score: number,
    feedback?: string,
  ): Observable<BackendEvaluationReview>;
}
