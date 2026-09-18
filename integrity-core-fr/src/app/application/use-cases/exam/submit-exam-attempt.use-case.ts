import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExamRepository } from '../../../domain/ports/exam.repository';
import { ExamAnswer } from '../../../domain/models/exam/exam-answer.model';
import { ExamAttempt } from '../../../domain/models/exam/exam-attempt.model';

@Injectable({ providedIn: 'root' })
export class SubmitExamAttemptUseCase {
  constructor(private readonly examRepository: ExamRepository) {}

  execute(examId: string, answers: ExamAnswer[]): Observable<ExamAttempt> {
    return this.examRepository.submitAttempt(examId, answers);
  }
}
