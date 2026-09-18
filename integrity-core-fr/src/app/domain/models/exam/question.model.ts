import { QuestionType } from './question-type.enum';
import { QuestionOption } from './question-option.model';

export interface Question {
  id: string;
  type: QuestionType;
  statement: string;
  points: number;
  // Solo aplica cuando type === MULTIPLE_CHOICE
  options?: QuestionOption[];
  // Solo aplica cuando type === MULTIPLE_CHOICE. IMPORTANTE: hoy esto viaja
  // completo porque no hay backend — cuando exista, el endpoint que sirve
  // el examen al alumno NO debe incluir este campo.
  correctOptionId?: string;
}
