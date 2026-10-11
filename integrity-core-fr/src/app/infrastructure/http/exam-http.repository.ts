import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, of, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ExamRepository } from '../../domain/ports/exam.repository';
import { Exam } from '../../domain/models/exam/exam.model';
import { ExamAnswer } from '../../domain/models/exam/exam-answer.model';
import { ExamAttempt } from '../../domain/models/exam/exam-attempt.model';
import { QuestionType } from '../../domain/models/exam/question-type.enum';
import { BackendExam, BackendQuestion, BackendQuestionType } from '../../domain/models/evaluator/evaluator.model';

@Injectable({ providedIn: 'root' })
export class ExamHttpRepository extends ExamRepository {
  private readonly baseUrl = `${environment.apiUrl}/api/evaluator`;
  private readonly http = inject(HttpClient)
  
  constructor() {
    super();
  }

  list(): Observable<Exam[]> {
    return this.http.get<BackendExam[]>(`${this.baseUrl}/exams`).pipe(
      map((exams) => exams.map((exam) => this.toExam(exam))),
    );
  }

  getById(id: string): Observable<Exam | undefined> {
    return this.http.get<BackendExam>(`${this.baseUrl}/exams/${id}`).pipe(
      map((exam) => this.toExam(exam)),
    );
  }

  create(exam: Omit<Exam, 'id'>): Observable<Exam> {
    return this.http.post<BackendExam>(`${this.baseUrl}/exams`, {
      title: exam.title,
      description: exam.description,
      durationMinutes: exam.durationMinutes,
      questions: exam.questions.map((question) => ({
        prompt: question.statement,
        type: this.toBackendQuestionType(question.type),
        options: (question.options ?? []).map((option) => ({
          text: option.text,
          correct: option.id === question.correctOptionId,
        })),
        correctAnswerText: question.type === QuestionType.OPEN ? null : undefined,
        points: question.points,
      })),
    }).pipe(
      map((created) => this.toExam(created)),
    );
  }

  update(exam: Exam): Observable<Exam> {
    // El backend reemplaza el listado completo de preguntas; solo permite editar
    // exámenes en borrador (en publicados/cerrados rechaza el cambio).
    return this.http.put<BackendExam>(`${this.baseUrl}/exams/${exam.id}`, {
      title: exam.title,
      description: exam.description,
      durationMinutes: exam.durationMinutes,
      questions: exam.questions.map((question) => ({
        prompt: question.statement,
        type: this.toBackendQuestionType(question.type),
        options: (question.options ?? []).map((option) => ({
          text: option.text,
          correct: option.id === question.correctOptionId,
        })),
        correctAnswerText: question.type === QuestionType.OPEN ? null : undefined,
        points: question.points,
      })),
    }).pipe(
      map((updated) => this.toExam(updated)),
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/exams/${id}`);
  }

  submitAttempt(examId: string, answers: ExamAnswer[]): Observable<ExamAttempt> {
    return of({ examId, answers }).pipe(
      switchMap(() => throwError(() => new Error(
        `El examen ${examId} requiere un código de acceso para iniciar el intento.`,
      ))),
    );
  }

  private addQuestions(created: BackendExam, exam: Omit<Exam, 'id'>): Observable<Exam> {
    if (exam.questions.length === 0) {
      return of(this.toExam(created));
    }

    const requests = exam.questions.map((question) => this.http.post<BackendExam>(
      `${this.baseUrl}/exams/${created.id}/questions`,
      {
        prompt: question.statement,
        type: this.toBackendQuestionType(question.type),
        options: (question.options ?? []).map((option) => ({
          text: option.text,
          correct: option.id === question.correctOptionId,
        })),
        correctAnswerText: question.type === QuestionType.OPEN ? null : undefined,
      },
    ));

    return forkJoin(requests).pipe(
      map((updated) => this.toExam(updated[updated.length - 1] ?? created)),
    );
  }

  private toExam(exam: BackendExam): Exam {
    return {
      id: exam.id,
      title: exam.title,
      description: exam.description ?? '',
      durationMinutes: exam.durationMinutes,
      status: exam.status,
      questions: (exam.questions ?? []).map((question) => this.toQuestion(question)),
    };
  }

  private toQuestion(question: BackendQuestion) {
    const correctOption = question.options?.find((option) => option.correct);
    return {
      id: question.id,
      type: question.type === 'OPEN_ENDED' ? QuestionType.OPEN : QuestionType.MULTIPLE_CHOICE,
      statement: question.prompt,
      points: question.points ?? 1,
      options: question.options?.map((option) => ({ id: option.id, text: option.text })),
      correctOptionId: correctOption?.id,
    };
  }

  private toBackendQuestionType(type: QuestionType): BackendQuestionType {
    // El editor solo permite marcar UNA respuesta correcta, o sea, opción única.
    return type === QuestionType.OPEN ? 'OPEN_ENDED' : 'SINGLE_CHOICE';
  }
}
