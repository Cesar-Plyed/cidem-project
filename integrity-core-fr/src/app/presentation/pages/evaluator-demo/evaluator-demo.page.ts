import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonList,
  IonItem,
  IonInput,
  IonButton,
  IonText,
} from '@ionic/angular';
import { CreateRemoteExamUseCase } from '../../../application/use-cases/evaluator/create-remote-exam.use-case';
import { GenerateAccessCodeUseCase } from '../../../application/use-cases/evaluator/generate-access-code.use-case';
import { JoinRemoteExamUseCase } from '../../../application/use-cases/evaluator/join-remote-exam.use-case';
import { SubmitRemoteAnswerUseCase } from '../../../application/use-cases/evaluator/submit-remote-answer.use-case';
import { SubmitRemoteAttemptUseCase } from '../../../application/use-cases/evaluator/submit-remote-attempt.use-case';
import { SessionService } from '../../../application/session/session.service';

/**
 * Página de VERIFICACIÓN, no de producto. Ejercita las 5 operaciones que
 * el backend real de evaluator soporta hoy, paso a paso, mostrando la
 * respuesta cruda de cada una. Sirve para confirmar que la conexión al
 * backend funciona mientras el resto de endpoints (listar, obtener con
 * preguntas, publicar) no existen todavía.
 *
 * No está enlazada desde ninguna otra pantalla a propósito — entra
 * directo por /evaluator-demo mientras la usas para probar.
 */
@Component({
  selector: 'app-evaluator-demo',
  templateUrl: 'evaluator-demo.page.html',
  styleUrls: ['evaluator-demo.page.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardContent,
    IonList,
    IonItem,
    IonInput,
    IonButton,
    IonText,
  ],
})
export class EvaluatorDemoPage {
  currentUserId: string | null = null;

  readonly createExamForm = this.fb.group({
    title: ['Examen de prueba', Validators.required],
    description: ['Creado desde la pantalla de verificación'],
  });
  createExamResult: unknown = null;
  createExamError: string | null = null;

  readonly accessCodeForm = this.fb.group({
    examId: ['', Validators.required],
    expiresInMinutes: [20, [Validators.required, Validators.min(1)]],
    code: [''],
  });
  accessCodeResult: unknown = null;
  accessCodeError: string | null = null;

  readonly joinForm = this.fb.group({
    examId: ['', Validators.required],
    code: ['', Validators.required],
    studentId: ['', Validators.required],
  });
  joinResult: unknown = null;
  joinError: string | null = null;

  readonly answerForm = this.fb.group({
    attemptId: ['', Validators.required],
    questionId: ['', Validators.required],
    selectedOptionIds: [''], // separados por coma
    openTextAnswer: [''],
  });
  answerResult: unknown = null;
  answerError: string | null = null;

  readonly submitForm = this.fb.group({
    attemptId: ['', Validators.required],
  });
  submitResult: unknown = null;
  submitError: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly sessionService: SessionService,
    private readonly createRemoteExamUseCase: CreateRemoteExamUseCase,
    private readonly generateAccessCodeUseCase: GenerateAccessCodeUseCase,
    private readonly joinRemoteExamUseCase: JoinRemoteExamUseCase,
    private readonly submitRemoteAnswerUseCase: SubmitRemoteAnswerUseCase,
    private readonly submitRemoteAttemptUseCase: SubmitRemoteAttemptUseCase,
  ) {
    this.sessionService.getUserId().then((id) => {
      this.currentUserId = id;
      if (id) {
        this.joinForm.patchValue({ studentId: id });
      }
    });
  }

  createExam(): void {
    if (this.createExamForm.invalid || !this.currentUserId) {
      this.createExamError = !this.currentUserId
        ? 'Necesitas iniciar sesión primero (falta el userId del token).'
        : 'Revisa los campos.';
      return;
    }
    this.createExamError = null;
    const { title, description } = this.createExamForm.getRawValue();
    this.createRemoteExamUseCase.execute(title!, description || undefined, this.currentUserId).subscribe({
      next: (exam) => {
        this.createExamResult = exam;
        // precarga el examId en el siguiente paso para no copiar/pegar
        this.accessCodeForm.patchValue({ examId: exam.id });
      },
      error: (err: HttpErrorResponse) => (this.createExamError = this.formatError(err)),
    });
  }

  generateAccessCode(): void {
    if (this.accessCodeForm.invalid) {
      this.accessCodeError = 'Revisa los campos.';
      return;
    }
    this.accessCodeError = null;
    const { examId, expiresInMinutes, code } = this.accessCodeForm.getRawValue();
    this.generateAccessCodeUseCase.execute(examId!, expiresInMinutes!, code || undefined).subscribe({
      next: (result) => {
        this.accessCodeResult = result;
        this.joinForm.patchValue({ examId: examId!, code: result.code });
      },
      error: (err: HttpErrorResponse) => (this.accessCodeError = this.formatError(err)),
    });
  }

  joinExam(): void {
    if (this.joinForm.invalid) {
      this.joinError = 'Revisa los campos.';
      return;
    }
    this.joinError = null;
    const { examId, code, studentId } = this.joinForm.getRawValue();
    this.joinRemoteExamUseCase.execute(examId!, code!, studentId!).subscribe({
      next: (attempt) => {
        this.joinResult = attempt;
        this.answerForm.patchValue({ attemptId: attempt.id });
        this.submitForm.patchValue({ attemptId: attempt.id });
      },
      error: (err: HttpErrorResponse) => (this.joinError = this.formatError(err)),
    });
  }

  submitAnswer(): void {
    if (this.answerForm.invalid) {
      this.answerError = 'Revisa los campos.';
      return;
    }
    this.answerError = null;
    const { attemptId, questionId, selectedOptionIds, openTextAnswer } = this.answerForm.getRawValue();
    const optionIds = (selectedOptionIds || '')
      .split(',')
      .map((id) => id.trim())
      .filter((id) => id.length > 0);

    this.submitRemoteAnswerUseCase
      .execute(attemptId!, questionId!, optionIds, openTextAnswer || undefined)
      .subscribe({
        next: (answer) => (this.answerResult = answer),
        error: (err: HttpErrorResponse) => (this.answerError = this.formatError(err)),
      });
  }

  submitAttempt(): void {
    if (this.submitForm.invalid) {
      this.submitError = 'Revisa el attemptId.';
      return;
    }
    this.submitError = null;
    const { attemptId } = this.submitForm.getRawValue();
    this.submitRemoteAttemptUseCase.execute(attemptId!).subscribe({
      next: (attempt) => (this.submitResult = attempt),
      error: (err: HttpErrorResponse) => (this.submitError = this.formatError(err)),
    });
  }

  private formatError(err: HttpErrorResponse): string {
    const body = err.error as { message?: string } | undefined;
    return body?.message ?? `Error ${err.status}: ${err.statusText}`;
  }
}
