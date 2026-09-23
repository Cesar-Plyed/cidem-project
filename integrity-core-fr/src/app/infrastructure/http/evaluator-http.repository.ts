import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EvaluatorRepository } from '../../domain/ports/evaluator.repository';
import {
  BackendExam,
  BackendExamAccessCode,
  BackendExamAnswer,
  BackendExamAttempt,
  BackendExamAverage,
  BackendExamResult,
  BackendEvaluationReview,
  BackendOpenEndedReview,
} from '../../domain/models/evaluator/evaluator.model';
import { AttemptExam } from '../../domain/models/evaluator/attempt-exam.model';

/**
 * Adaptador contra el módulo evaluator del backend. El servidor obtiene el
 * usuario autenticado desde el JWT; los identificadores enviados por el
 * cliente no sustituyen esa identidad.
 */
@Injectable({ providedIn: 'root' })
export class EvaluatorHttpRepository extends EvaluatorRepository {
  private readonly baseUrl = `${environment.apiUrl}/api/evaluator`;
  private readonly http = inject(HttpClient)

  constructor() {
    super();
  }

  createExam(title: string, description: string | undefined, createdByUserId: string): Observable<BackendExam> {
    return this.http.post<BackendExam>(`${this.baseUrl}/exams`, {
      title,
      description,
      createdByUserId,
    });
  }

  publishExam(examId: string): Observable<BackendExam> {
    return this.http.post<BackendExam>(`${this.baseUrl}/exams/${examId}/publish`, null);
  }

  generateAccessCode(
    examId: string,
    expiresInMinutes: number,
    code?: string,
  ): Observable<BackendExamAccessCode> {
    let params = new HttpParams().set('expiresInMinutes', expiresInMinutes);
    if (code) {
      params = params.set('code', code);
    }
    return this.http.post<BackendExamAccessCode>(
      `${this.baseUrl}/exams/${examId}/access-code`,
      null,
      { params },
    );
  }

  joinExam(examId: string, code: string, studentId: string): Observable<BackendExamAttempt> {
    return this.http.post<BackendExamAttempt>(`${this.baseUrl}/attempts/join`, {
      examId,
      code,
      studentId,
    });
  }

  joinExamByCode(code: string): Observable<BackendExamAttempt> {
    return this.http.post<BackendExamAttempt>(`${this.baseUrl}/attempts/join-by-code`, { code });
  }

  getAttemptExam(attemptId: string): Observable<AttemptExam> {
    return this.http.get<AttemptExam>(`${this.baseUrl}/attempts/${attemptId}/exam`);
  }

  submitAnswer(
    attemptId: string,
    questionId: string,
    selectedOptionIds: string[],
    openTextAnswer?: string,
  ): Observable<BackendExamAnswer> {
    return this.http.post<BackendExamAnswer>(`${this.baseUrl}/attempts/${attemptId}/answers`, {
      questionId,
      selectedOptionIds,
      openTextAnswer,
    });
  }

  submitAttempt(attemptId: string): Observable<BackendExamAttempt> {
    return this.http.post<BackendExamAttempt>(`${this.baseUrl}/attempts/${attemptId}/submit`, null);
  }

  getExamResults(examId: string): Observable<BackendExamResult[]> {
    return this.http.get<BackendExamResult[]>(`${this.baseUrl}/exams/${examId}/results`);
  }

  getAttemptResult(attemptId: string): Observable<BackendExamResult> {
    return this.http.get<BackendExamResult>(`${this.baseUrl}/attempts/${attemptId}/result`);
  }

  getExamAverage(examId: string): Observable<BackendExamAverage> {
    return this.http.get<BackendExamAverage>(`${this.baseUrl}/exams/${examId}/average`);
  }

  listOpenEndedReviews(
    examId: string,
    onlyPending = true,
  ): Observable<BackendOpenEndedReview[]> {
    const params = new HttpParams().set('pending', onlyPending);
    return this.http.get<BackendOpenEndedReview[]>(
      `${this.baseUrl}/exams/${examId}/reviews/open-ended`,
      { params },
    );
  }

  reviewOpenEndedAnswer(
    answerId: string,
    score: number,
    feedback?: string,
  ): Observable<BackendEvaluationReview> {
    return this.http.put<BackendEvaluationReview>(
      `${this.baseUrl}/reviews/open-ended/${answerId}`,
      { score, feedback: feedback ?? null },
    );
  }
}
