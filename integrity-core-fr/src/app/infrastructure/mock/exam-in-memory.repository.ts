import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { ExamRepository } from '../../domain/ports/exam.repository';
import { Exam } from '../../domain/models/exam/exam.model';
import { ExamAnswer } from '../../domain/models/exam/exam-answer.model';
import { ExamAttempt } from '../../domain/models/exam/exam-attempt.model';
import { QuestionType } from '../../domain/models/exam/question-type.enum';

/**
 * Implementación TEMPORAL en memoria: simula el backend mientras no está
 * listo. Los datos viven solo en esta instancia (se pierden al recargar
 * la página) y el `delay(150)` simula la latencia de una llamada real
 * para que la UI de loading se pueda probar desde ya.
 *
 * Cuando el backend esté disponible, esta clase se reemplaza por un
 * ExamHttpRepository con el mismo contrato (ExamRepository) — solo cambia
 * el `provide` en main.ts.
 */
@Injectable({ providedIn: 'root' })
export class ExamInMemoryRepository extends ExamRepository {
  private nextId = 2;

  private exams: Exam[] = [
    {
      id: '1',
      title: 'Examen de ejemplo: Fundamentos de Redes',
      description: 'Examen de práctica para probar la vista de alumno y docente.',
      durationMinutes: 30,
      questions: [
        {
          id: '1-1',
          type: QuestionType.MULTIPLE_CHOICE,
          statement: '¿Qué capa del modelo OSI se encarga del direccionamiento IP?',
          points: 1,
          options: [
            { id: 'a', text: 'Capa de enlace de datos' },
            { id: 'b', text: 'Capa de red' },
            { id: 'c', text: 'Capa de transporte' },
            { id: 'd', text: 'Capa de aplicación' },
          ],
          correctOptionId: 'b',
        },
        {
          id: '1-2',
          type: QuestionType.OPEN,
          statement: 'Explica brevemente la diferencia entre TCP y UDP.',
          points: 2,
        },
      ],
    },
  ];

  private attempts: ExamAttempt[] = [];

  list(): Observable<Exam[]> {
    return of(this.exams).pipe(delay(150));
  }

  getById(id: string): Observable<Exam | undefined> {
    return of(this.exams.find((exam) => exam.id === id)).pipe(delay(150));
  }

  create(exam: Omit<Exam, 'id'>): Observable<Exam> {
    const created: Exam = { ...exam, id: String(this.nextId++) };
    this.exams = [...this.exams, created];
    return of(created).pipe(delay(150));
  }

  update(exam: Exam): Observable<Exam> {
    this.exams = this.exams.map((existing) => (existing.id === exam.id ? exam : existing));
    return of(exam).pipe(delay(150));
  }

  delete(id: string): Observable<void> {
    this.exams = this.exams.filter((exam) => exam.id !== id);
    return of(undefined).pipe(delay(150));
  }

  submitAttempt(examId: string, answers: ExamAnswer[]): Observable<ExamAttempt> {
    const attempt: ExamAttempt = {
      id: `${examId}-${Date.now()}`,
      examId,
      submittedAt: new Date().toISOString(),
      answers,
    };
    this.attempts = [...this.attempts, attempt];
    return of(attempt).pipe(delay(150));
  }
}
