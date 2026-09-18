import { Observable } from 'rxjs';
import { Exam } from '../models/exam/exam.model';
import { ExamAnswer } from '../models/exam/exam-answer.model';
import { ExamAttempt } from '../models/exam/exam-attempt.model';

/**
 * Puerto de salida para exámenes. Hoy la única implementación es un
 * repositorio en memoria (infrastructure/mock/exam-in-memory.repository.ts)
 * porque el backend todavía no existe. Cuando esté listo, se agrega un
 * ExamHttpRepository que implemente este mismo contrato y se cambia el
 * provider en main.ts — application/ y presentation/ no se tocan.
 */
export abstract class ExamRepository {
  abstract list(): Observable<Exam[]>;
  abstract getById(id: string): Observable<Exam | undefined>;
  abstract create(exam: Omit<Exam, 'id'>): Observable<Exam>;
  abstract update(exam: Exam): Observable<Exam>;
  abstract delete(id: string): Observable<void>;
  abstract submitAttempt(examId: string, answers: ExamAnswer[]): Observable<ExamAttempt>;
}
