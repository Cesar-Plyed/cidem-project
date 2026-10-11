// Estos modelos son un espejo 1:1 de lo que el backend real serializa hoy
// (verificado contra el código fuente de evaluator/domain/model, no solo
// contra documentación). OJO: los nombres de campo son distintos a los que
// usa el mock de la vista docente/alumno (domain/models/exam/*):
//   - "prompt" en vez de "statement"
//   - cada opción trae su propio flag "correct" (boolean), no hay un
//     "correctOptionId" separado
//   - hay 3 tipos de pregunta, no 2 (SINGLE_CHOICE se suma a MULTIPLE_CHOICE
//     y OPEN_ENDED)
// Cuando el backend exponga los endpoints de lectura que faltan, este es el
// modelo a usar para reemplazar domain/models/exam/*.

export type BackendQuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'OPEN_ENDED';

export type BackendExamStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';

export interface BackendQuestionOption {
  id: string;
  text: string;
  correct: boolean;
}

export interface BackendQuestion {
  id: string;
  prompt: string;
  type: BackendQuestionType;
  options: BackendQuestionOption[];
  correctAnswerText: string | null;
  /** Puntos de la pregunta (1..10); la suma del examen no puede exceder 10. */
  points: number;
}

export interface BackendExam {
  id: string;
  title: string;
  description: string | null;
  createdByUserId: string;
  createdAt: string; // ISO-8601
  durationMinutes: number;
  questions: BackendQuestion[]; // hoy siempre viene vacío: no hay forma de mandar preguntas al crear
  status: BackendExamStatus;
}

export interface BackendExamAccessCode {
  id: string;
  examId: string;
  code: string;
  expiresAt: string; // ISO-8601
  active: boolean;
}

export interface BackendExamAttempt {
  id: string;
  examId: string;
  studentId: string;
  startedAt: string; // ISO-8601
  submittedAt: string | null;
  submitted: boolean;
  integrityFailed: boolean;
}

export interface BackendExamAnswer {
  id: string;
  attemptId: string;
  questionId: string;
  selectedOptionIds: string[];
  openTextAnswer: string | null;
}

export interface BackendEvaluationReview {
  id: string;
  answerId: string;
  reviewerId: string;
  score: number;
  feedback: string | null;
  createdAt: string; // ISO-8601
  updatedAt: string; // ISO-8601
}

export interface BackendOpenEndedReview {
  answerId: string;
  attemptId: string;
  studentId: string;
  questionId: string;
  questionPrompt: string;
  openTextAnswer: string | null;
  /** Puntos de la pregunta: tope de la puntuación al revisar. */
  questionPoints: number | null;
  score: number | null;
  feedback: string | null;
  reviewerId: string | null;
  reviewed: boolean;
  createdAt: string | null; // ISO-8601
  updatedAt: string | null; // ISO-8601
}

export interface BackendQuestionOptionResult {
  id: string;
  text: string;
  correct: boolean;
}

export interface BackendQuestionResult {
  questionId: string;
  /** Solo viene cuando el alumno respondió; sirve para calificar la respuesta abierta en línea. */
  answerId: string | null;
  statement: string;
  questionType: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'OPEN_ENDED';
  selectedOptionIds: string[];
  openTextAnswer: string | null;
  options: BackendQuestionOptionResult[];
  answered: boolean;
  score: number;
  /** Puntos máximos de la pregunta: la respuesta abierta no puede superarlos. */
  points: number;
  pendingReview: boolean;
  feedback: string | null;
}

export interface BackendExamResult {
  attemptId: string;
  examId: string;
  studentId: string;
  studentEmail: string | null;
  studentFirstName: string | null;
  studentLastName: string | null;
  studentPhone: string | null;
  score: number;
  maxScore: number;
  pendingReview: boolean;
  integrityFailed: boolean;
  questions: BackendQuestionResult[];
}

export interface BackendExamAverage {
  examId: string;
  examTitle: string;
  totalAttempts: number;
  gradedAttempts: number;
  pendingReviewCount: number;
  averageScore: number;
  maxScore: number;
  averagePercentage: number;
}

export interface StudentExamHistoryEntry {
  attemptId: string;
  examId: string;
  examTitle: string;
  startedAt: string;
  submittedAt: string;
  score: number;
  maxScore: number;
  pendingReview: boolean;
  integrityFailed: boolean;
}
