import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EvaluatorRepository } from '../../domain/ports/evaluator.repository';
import {
  BackendExam,
  BackendExamAccessCode,
  BackendExamAnswer,
  BackendExamAttempt,
} from '../../domain/models/evaluator/evaluator.model';

/**
 * Adaptador real contra el módulo evaluator del backend
 * (edu.cidemags.integrity_core.evaluator). Cada método aquí corresponde
 * 1:1 a un @PostMapping que sí existe en el código fuente:
 *   - ExamController:            POST /exams, POST /exams/{id}/access-code
 *   - ExamAttemptController:     POST /attempts/join, /submit, /answers
 *
 * No hay GET en ningún controller del módulo — por eso este repositorio
 * no tiene (ni puede tener) list()/getById(). Cuando el backend agregue
 * esos endpoints, se extiende esta clase y el puerto EvaluatorRepository.
 */
@Injectable({ providedIn: 'root' })
export class EvaluatorHttpRepository extends EvaluatorRepository {
  private readonly baseUrl = `${environment.apiUrl}/api/evaluator`;

  constructor(private readonly http: HttpClient) {
    super();
  }

  createExam(title: string, description: string | undefined, createdByUserId: string): Observable<BackendExam> {
    return this.http.post<BackendExam>(`${this.baseUrl}/exams`, {
      title,
      description,
      createdByUserId,
    });
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
}
