import { BackendExamStatus } from '../evaluator/evaluator.model';
import { Question } from './question.model';

export interface Exam {
  id: string;
  title: string;
  description: string;
  durationMinutes: number;
  questions: Question[];
  status?: BackendExamStatus;
  /** Nº de preguntas según el backend (listado sin preguntas embebidas). */
  questionCount?: number;
  /** Nombre del creador resuelto por el backend (listado). */
  createdByName?: string;
  /** true si el examen puede tomarse desde un navegador; false exige la app de escritorio. */
  browserAllowed?: boolean;
}
