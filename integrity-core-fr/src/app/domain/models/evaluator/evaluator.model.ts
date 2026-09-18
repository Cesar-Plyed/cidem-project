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
}

export interface BackendExam {
  id: string;
  title: string;
  description: string | null;
  createdByUserId: string;
  createdAt: string; // ISO-8601
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
}

export interface BackendExamAnswer {
  id: string;
  attemptId: string;
  questionId: string;
  selectedOptionIds: string[];
  openTextAnswer: string | null;
}
