import { Observable } from 'rxjs';
import {
  BackendExam,
  BackendExamAccessCode,
  BackendExamAnswer,
  BackendExamAttempt,
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
 *
 * NO incluye listar exámenes, obtener un examen por id, ni mandar
 * preguntas — porque el backend no tiene esos endpoints. Agregar métodos
 * aquí "por si acaso" haría creer que están conectados cuando no lo están.
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
}
