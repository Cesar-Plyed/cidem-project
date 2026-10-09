import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import {
  IonContent,
  IonIcon,
} from '@ionic/angular';
import { addIcons } from 'ionicons';
import { addOutline, trashOutline } from 'ionicons/icons';
import { GetExamUseCase } from '../../../application/use-cases/exam/get-exam.use-case';
import { CreateExamUseCase } from '../../../application/use-cases/exam/create-exam.use-case';
import { UpdateExamUseCase } from '../../../application/use-cases/exam/update-exam.use-case';
import { QuestionType } from '../../../domain/models/exam/question-type.enum';
import { Exam } from '../../../domain/models/exam/exam.model';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';

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
    IonContent,
    IonIcon,
    AppHeaderComponent,
  ],
})
export class ExamEditorPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly getExamUseCase = inject(GetExamUseCase);
  private readonly createExamUseCase = inject(CreateExamUseCase);
  private readonly updateExamUseCase = inject(UpdateExamUseCase);

  readonly QuestionType = QuestionType;

  examId: string | null = null;
  // Signals: sin zone.js, Angular solo refresca la vista cuando cambia un signal.
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    title: ['', Validators.required],
    description: ['', Validators.required],
    durationMinutes: [30, [Validators.required, Validators.min(1)]],
    questions: this.fb.array<FormGroup>([]),
  });

  constructor() {
    addIcons({ addOutline, trashOutline });
  }

  get questions(): FormArray<FormGroup> {
    return this.form.controls.questions;
  }

  get title() {
    return this.form.controls.title;
  }

  get description() {
    return this.form.controls.description;
  }

  get durationMinutes() {
    return this.form.controls.durationMinutes;
  }

  questionCtrl(index: number, name: string) {
    return this.questions.at(index)?.get(name);
  }

  questionOptions(index: number): FormArray<FormGroup> {
    return this.optionsOf(index);
  }

  optionTextTouched(questionIndex: number, optionIndex: number): boolean {
    const control = this.optionsOf(questionIndex).at(optionIndex)?.get('text');
    return control ? control.touched && control.invalid : false;
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
    this.loading.set(true);
    this.getExamUseCase.execute(id).subscribe({
      next: (exam) => {
        this.loading.set(false);
        if (!exam) {
          this.errorMessage.set('No se encontró el examen.');
          return;
        }
        this.patchForm(exam);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(`No se pudo cargar el examen (${err.status}).`);
      },
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

  private buildQuestionGroup(
    type: QuestionType = QuestionType.MULTIPLE_CHOICE,
  ): FormGroup {
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

  private buildOptionGroup(
    id: string = crypto.randomUUID(),
    text: string = '',
  ): FormGroup {
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
    return this.questions.at(questionIndex).controls[
      'options'
    ] as FormArray<FormGroup>;
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
      options.clear();
      questionGroup.patchValue({ correctOptionId: null });
    }
  }

  isMultipleChoice(questionIndex: number): boolean {
    return (
      this.questions.at(questionIndex).value['type'] ===
      QuestionType.MULTIPLE_CHOICE
    );
  }

  save(event?: Event): void {
    if (this.saving()) return;
    event?.preventDefault();
    if (this.form.invalid || this.questions.length === 0) {
      this.form.markAllAsTouched();
      const invalidFields = this.collectInvalidFields();
      const message = invalidFields.length > 0
        ? `Revisa los siguientes campos: ${invalidFields.join(', ')}.`
        : 'Revisa los campos marcados.';
      console.log('[ExamEditor] Campos inválidos:', invalidFields);
      this.errorMessage.set(
        this.questions.length === 0
          ? 'Agrega al menos una pregunta.'
          : message,
      );
      return;
    }

    const missingCorrectAnswer = this.questions.controls.some(
      (group, index) =>
        this.isMultipleChoice(index) && !group.value['correctOptionId'],
    );
    if (missingCorrectAnswer) {
      this.errorMessage.set(
        'Marca la respuesta correcta en cada pregunta de opción múltiple.',
      );
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);

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
        options:
          q.type === QuestionType.MULTIPLE_CHOICE ? q.options : undefined,
        correctOptionId:
          q.type === QuestionType.MULTIPLE_CHOICE
            ? (q.correctOptionId ?? undefined)
            : undefined,
      })),
    };

    const request$ = this.examId
      ? this.updateExamUseCase.execute({ id: this.examId, ...examPayload })
      : this.createExamUseCase.execute(examPayload);

    request$.subscribe({
      next: () => {
        // "saving" sigue activo hasta terminar de navegar, para no aceptar otro clic en medio.
        void this.router.navigate(['/exams']).finally(() => this.saving.set(false));
      },
      error: (err: HttpErrorResponse) => {
        // Deja el detalle del backend a la vista: abre la consola (F12) para ver el cuerpo completo.
        console.error('Error al guardar el examen', err.status, err.error);
        this.saving.set(false);
        const serverMessage = (err.error as { message?: string } | null)?.message;
        this.errorMessage.set(
          `No se pudo guardar el examen (HTTP ${err.status}).${serverMessage ? ' ' + serverMessage : ''}`,
        );
      },
    });
  }

  private collectInvalidFields(): string[] {
    const fields: string[] = [];
    Object.keys(this.form.controls).forEach((key) => {
      const control = this.form.get(key);
      if (control && control.invalid) {
        console.log(`[ExamEditor] Control "${key}" inválido:`, control.errors, 'valor:', control.value);
        fields.push(key);
      }
    });
    this.questions.controls.forEach((group, index) => {
      Object.keys(group.controls).forEach((key) => {
        const control = group.get(key);
        if (control && control.invalid) {
          const label = this.fieldLabel(key);
          console.log(`[ExamEditor] questions[${index}].${key} inválido:`, control.errors, 'valor:', control.value);
          fields.push(`Pregunta ${index + 1}: ${label}`);
        }
      });
      const options = group.get('options') as FormArray<FormGroup>;
      options?.controls.forEach((opt, optIndex) => {
        Object.keys(opt.controls).forEach((key) => {
          const control = opt.get(key);
          if (control && control.invalid) {
            console.log(`[ExamEditor] questions[${index}].options[${optIndex}].${key} inválido:`, control.errors, 'valor:', control.value);
            fields.push(`Pregunta ${index + 1}, opción ${optIndex + 1}: ${this.fieldLabel(key)}`);
          }
        });
      });
    });
    return fields;
  }

  private fieldLabel(key: string): string {
    const labels: Record<string, string> = {
      title: 'Título',
      description: 'Descripción',
      durationMinutes: 'Duración',
      type: 'Tipo de pregunta',
      statement: 'Enunciado',
      points: 'Puntos',
      text: 'Texto de opción',
    };
    return labels[key] ?? key;
  }
}
