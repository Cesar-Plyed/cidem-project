import { ExamAnswer } from './exam-answer.model';

export interface ExamAttempt {
  id: string;
  examId: string;
  submittedAt: string; // ISO-8601
  answers: ExamAnswer[];
}
