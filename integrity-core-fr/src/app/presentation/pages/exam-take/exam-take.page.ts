import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonButtons,
  IonBackButton,
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonRadioGroup,
  IonRadio,
  IonItem,
  IonLabel,
  IonTextarea,
  IonButton,
  IonText,
  IonSpinner,
  IonBadge,
} from '@ionic/angular';
import { GetExamUseCase } from '../../../application/use-cases/exam/get-exam.use-case';
import { SubmitExamAttemptUseCase } from '../../../application/use-cases/exam/submit-exam-attempt.use-case';
import { Exam } from '../../../domain/models/exam/exam.model';
import { QuestionType } from '../../../domain/models/exam/question-type.enum';
import { ExamAnswer } from '../../../domain/models/exam/exam-answer.model';

@Component({
  selector: 'app-exam-take',
  templateUrl: 'exam-take.page.html',
  styleUrls: ['exam-take.page.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButtons,
    IonBackButton,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardContent,
    IonRadioGroup,
    IonRadio,
    IonItem,
    IonLabel,
    IonTextarea,
    IonButton,
    IonText,
    IonSpinner,
    IonBadge,
  ],
})
export class ExamTakePage implements OnInit {
  readonly QuestionType = QuestionType;

  exam: Exam | null = null;
  loading = true;
  submitting = false;
  submitted = false;
  errorMessage: string | null = null;

  form: FormGroup = this.fb.group({});

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly getExamUseCase: GetExamUseCase,
    private readonly submitExamAttemptUseCase: SubmitExamAttemptUseCase,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Examen no encontrado.';
      this.loading = false;
      return;
    }

    this.getExamUseCase.execute(id).subscribe((exam) => {
      this.loading = false;
      if (!exam) {
        this.errorMessage = 'Examen no encontrado.';
        return;
      }
      this.exam = exam;
      this.buildAnswerForm(exam);
    });
  }

  private buildAnswerForm(exam: Exam): void {
    const group: Record<string, unknown> = {};
    exam.questions.forEach((question) => {
      group[question.id] =
        question.type === QuestionType.MULTIPLE_CHOICE
          ? [null, Validators.required]
          : ['', Validators.required];
    });
    this.form = this.fb.group(group);
  }

  submit(): void {
    if (!this.exam || this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage = 'Responde todas las preguntas antes de enviar.';
      return;
    }

    this.submitting = true;
    this.errorMessage = null;

    const raw = this.form.getRawValue() as Record<string, string | null>;
    const answers: ExamAnswer[] = this.exam.questions.map((question) => ({
      questionId: question.id,
      selectedOptionId:
        question.type === QuestionType.MULTIPLE_CHOICE ? raw[question.id] ?? undefined : undefined,
      openText: question.type === QuestionType.OPEN ? raw[question.id] ?? undefined : undefined,
    }));

    this.submitExamAttemptUseCase.execute(this.exam.id, answers).subscribe({
      next: () => {
        this.submitting = false;
        this.submitted = true;
      },
      error: () => {
        this.submitting = false;
        this.errorMessage = 'No se pudo enviar el examen. Intenta de nuevo.';
      },
    });
  }
}
