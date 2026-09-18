export interface ExamAnswer {
  questionId: string;
  selectedOptionId?: string; // para MULTIPLE_CHOICE
  openText?: string; // para OPEN
}
