import { BackendExamStatus } from '../evaluator/evaluator.model';
import { Question } from './question.model';

export interface Exam {
  id: string;
  title: string;
  description: string;
  durationMinutes: number;
  questions: Question[];
  status?: BackendExamStatus;
}
