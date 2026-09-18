import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonButtons,
  IonBackButton,
  IonContent,
  IonList,
  IonItem,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonButton,
  IonIcon,
  IonText,
  IonSpinner,
  IonRadioGroup,
  IonRadio,
  IonCard,
  IonCardContent,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import { addOutline, trashOutline } from 'ionicons/icons';
import { GetExamUseCase } from '../../../application/use-cases/exam/get-exam.use-case';
import { CreateExamUseCase } from '../../../application/use-cases/exam/create-exam.use-case';
import { UpdateExamUseCase } from '../../../application/use-cases/exam/update-exam.use-case';
import { QuestionType } from '../../../domain/models/exam/question-type.enum';
import { Exam } from '../../../domain/models/exam/exam.model';

interface QuestionFormValue {
  id: string;
  type: QuestionType;
  statement: string;
  points: number;
  correctOptionId: string | null;
  options: { id: string; text: string }[];
}

@Component({
  selector: 'app-exam-editor',
  templateUrl: 'exam-editor.page.html',
  styleUrls: ['exam-editor.page.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButtons,
    IonBackButton,
    IonContent,
    IonList,
    IonItem,
    IonInput,
    IonSelect,
    IonSelectOption,
    IonButton,
    IonIcon,
    IonText,
    IonSpinner,
    IonRadioGroup,
    IonRadio,
    IonCard,
    IonCardContent,
  ],
})
export class ExamEditorPage implements OnInit {
  readonly QuestionType = QuestionType;

  examId: string | null = null;
  loading = false;
  saving = false;
  errorMessage: string | null = null;

  readonly form = this.fb.group({
    title: ['', Validators.required],
    description: ['', Validators.required],
    durationMinutes: [30, [Validators.required, Validators.min(1)]],
    questions: this.fb.array<FormGroup>([]),
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly getExamUseCase: GetExamUseCase,
    private readonly createExamUseCase: CreateExamUseCase,
    private readonly updateExamUseCase: UpdateExamUseCase,
  ) {
    addIcons({ addOutline, trashOutline });
  }

  get questions(): FormArray<FormGroup> {
    return this.form.controls.questions;
  }

  ngOnInit(): void {
    this.examId = this.route.snapshot.paramMap.get('id');
    if (this.examId) {
      this.loadExam(this.examId);
    } else {
      this.addQuestion(); // arranca con una pregunta vacía para no dejar el form en blanco
    }
  }

  private loadExam(id: string): void {
    this.loading = true;
    this.getExamUseCase.execute(id).subscribe((exam) => {
      this.loading = false;
      if (!exam) {
        this.errorMessage = 'No se encontró el examen.';
        return;
      }
      this.patchForm(exam);
    });
  }

  private patchForm(exam: Exam): void {
    this.form.patchValue({
      title: exam.title,
      description: exam.description,
      durationMinutes: exam.durationMinutes,
    });
    this.questions.clear();
    exam.questions.forEach((question) => {
      const group = this.buildQuestionGroup(question.type);
      group.patchValue({
        id: question.id,
        type: question.type,
        statement: question.statement,
        points: question.points,
        correctOptionId: question.correctOptionId ?? null,
      });
      const optionsArray = group.controls['options'] as FormArray<FormGroup>;
      optionsArray.clear();
      (question.options ?? []).forEach((option) => {
        optionsArray.push(this.buildOptionGroup(option.id, option.text));
      });
      this.questions.push(group);
    });
  }

  private buildQuestionGroup(type: QuestionType = QuestionType.MULTIPLE_CHOICE): FormGroup {
    return this.fb.group({
      id: [crypto.randomUUID()],
      type: [type, Validators.required],
      statement: ['', Validators.required],
      points: [1, [Validators.required, Validators.min(1)]],
      correctOptionId: [null as string | null],
      options: this.fb.array<FormGroup>(
        type === QuestionType.MULTIPLE_CHOICE
          ? [this.buildOptionGroup(), this.buildOptionGroup()]
          : [],
      ),
    });
  }

  private buildOptionGroup(id: string = crypto.randomUUID(), text: string = ''): FormGroup {
    return this.fb.group({
      id: [id],
      text: [text, Validators.required],
    });
  }

  addQuestion(): void {
    this.questions.push(this.buildQuestionGroup());
  }

  removeQuestion(index: number): void {
    this.questions.removeAt(index);
  }

  optionsOf(questionIndex: number): FormArray<FormGroup> {
    return this.questions.at(questionIndex).controls['options'] as FormArray<FormGroup>;
  }

  addOption(questionIndex: number): void {
    this.optionsOf(questionIndex).push(this.buildOptionGroup());
  }

  removeOption(questionIndex: number, optionIndex: number): void {
    const options = this.optionsOf(questionIndex);
    const removedId = options.at(optionIndex).value.id;
    options.removeAt(optionIndex);

    // Si borramos la opción marcada como correcta, limpiamos la selección.
    const questionGroup = this.questions.at(questionIndex);
    if (questionGroup.value['correctOptionId'] === removedId) {
      questionGroup.patchValue({ correctOptionId: null });
    }
  }

  onTypeChange(questionIndex: number): void {
    const questionGroup = this.questions.at(questionIndex);
    const type = questionGroup.value['type'];
    const options = this.optionsOf(questionIndex);

    if (type === QuestionType.MULTIPLE_CHOICE && options.length === 0) {
      options.push(this.buildOptionGroup());
      options.push(this.buildOptionGroup());
    }
    if (type === QuestionType.OPEN) {
      questionGroup.patchValue({ correctOptionId: null });
    }
  }

  isMultipleChoice(questionIndex: number): boolean {
    return this.questions.at(questionIndex).value['type'] === QuestionType.MULTIPLE_CHOICE;
  }

  save(): void {
    if (this.form.invalid || this.questions.length === 0) {
      this.form.markAllAsTouched();
      this.errorMessage =
        this.questions.length === 0 ? 'Agrega al menos una pregunta.' : 'Revisa los campos marcados.';
      return;
    }

    const missingCorrectAnswer = this.questions.controls.some(
      (group, index) => this.isMultipleChoice(index) && !group.value['correctOptionId'],
    );
    if (missingCorrectAnswer) {
      this.errorMessage = 'Marca la respuesta correcta en cada pregunta de opción múltiple.';
      return;
    }

    this.saving = true;
    this.errorMessage = null;

    const raw = this.form.getRawValue();
    const questionValues = raw.questions as unknown as QuestionFormValue[];

    const examPayload = {
      title: raw.title!,
      description: raw.description!,
      durationMinutes: raw.durationMinutes!,
      questions: questionValues.map((q) => ({
        id: q.id,
        type: q.type,
        statement: q.statement,
        points: q.points,
        options: q.type === QuestionType.MULTIPLE_CHOICE ? q.options : undefined,
        correctOptionId: q.type === QuestionType.MULTIPLE_CHOICE ? (q.correctOptionId ?? undefined) : undefined,
      })),
    };

    const request$ = this.examId
      ? this.updateExamUseCase.execute({ id: this.examId, ...examPayload })
      : this.createExamUseCase.execute(examPayload);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.router.navigate(['/exams']);
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'No se pudo guardar el examen. Intenta de nuevo.';
      },
    });
  }
}
