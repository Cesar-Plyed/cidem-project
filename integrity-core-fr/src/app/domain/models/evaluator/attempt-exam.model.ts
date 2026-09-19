import { BackendQuestionType } from './evaluator.model';

// Espejo de AttemptExamResponse (backend): el examen tal como lo ve el alumno.
// A propósito no trae qué opciones son correctas.

export interface AttemptExamOption {
  id: string;
  text: string;
}

export interface AttemptExamQuestion {
  id: string;
  prompt: string;
  type: BackendQuestionType;
  options: AttemptExamOption[];
}

export interface AttemptExamAnswer {
  questionId: string;
  selectedOptionIds: string[];
  openTextAnswer: string | null;
}

export interface AttemptExam {
  attemptId: string;
  examId: string;
  title: string;
  description: string | null;
  startedAt: string; // ISO-8601
  submitted: boolean;
  questions: AttemptExamQuestion[];
  answers: AttemptExamAnswer[];
}
