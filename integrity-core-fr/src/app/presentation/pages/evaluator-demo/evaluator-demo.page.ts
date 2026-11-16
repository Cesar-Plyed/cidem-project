import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { IonContent } from '@ionic/angular';
import { CreateRemoteExamUseCase } from '../../../application/use-cases/evaluator/create-remote-exam.use-case';
import { GenerateAccessCodeUseCase } from '../../../application/use-cases/evaluator/generate-access-code.use-case';
import { JoinRemoteExamUseCase } from '../../../application/use-cases/evaluator/join-remote-exam.use-case';
import { SubmitRemoteAnswerUseCase } from '../../../application/use-cases/evaluator/submit-remote-answer.use-case';
import { SubmitRemoteAttemptUseCase } from '../../../application/use-cases/evaluator/submit-remote-attempt.use-case';
import { SessionService } from '../../../application/session/session.service';
import { AppHeaderComponent } from '../../components/app-header/app-header.component';
import { ExamDesktopRequiredComponent } from '../exam-desktop-required/exam-desktop-required.component';

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
    IonContent,
    AppHeaderComponent,
    ExamDesktopRequiredComponent,
  ],
})
export class EvaluatorDemoPage implements OnInit {
    private readonly fb = inject(FormBuilder)
    private readonly sessionService = inject(SessionService)
    private readonly createRemoteExamUseCase = inject(CreateRemoteExamUseCase)
    private readonly generateAccessCodeUseCase = inject(GenerateAccessCodeUseCase)
    private readonly joinRemoteExamUseCase = inject(JoinRemoteExamUseCase)
    private readonly submitRemoteAnswerUseCase = inject(SubmitRemoteAnswerUseCase)
    private readonly submitRemoteAttemptUseCase = inject(SubmitRemoteAttemptUseCase)

  // Señales: la app es zoneless, mutar campos planos dentro de subscribe()
  // no repinta la vista (los resultados y errores nunca se mostraban).
  readonly currentUserId = signal<string | null>(null);

  readonly createExamForm = this.fb.group({
    title: ['Examen de prueba', Validators.required],
    description: ['Creado desde la pantalla de verificación'],
  });
  readonly createExamResult = signal<unknown>(null);
  readonly createExamError = signal<string | null>(null);

  readonly accessCodeForm = this.fb.group({
    examId: ['', Validators.required],
    expiresInMinutes: [20, [Validators.required, Validators.min(1)]],
    code: [''],
  });
  readonly accessCodeResult = signal<unknown>(null);
  readonly accessCodeError = signal<string | null>(null);

  readonly joinForm = this.fb.group({
    examId: ['', Validators.required],
    code: ['', Validators.required],
    studentId: ['', Validators.required],
  });
  readonly joinResult = signal<unknown>(null);
  readonly joinError = signal<string | null>(null);
  readonly desktopRequired = signal(false);

  readonly answerForm = this.fb.group({
    attemptId: ['', Validators.required],
    questionId: ['', Validators.required],
    selectedOptionIds: [''], // separados por coma
    openTextAnswer: [''],
  });
  readonly answerResult = signal<unknown>(null);
  readonly answerError = signal<string | null>(null);

  readonly submitForm = this.fb.group({
    attemptId: ['', Validators.required],
  });
  readonly submitResult = signal<unknown>(null);
  readonly submitError = signal<string | null>(null);

  // El id de sesión solo se puede leer al iniciar; el callback escribe en
  // signals (la app es zoneless, un campo plano no repintaría la vista).
  ngOnInit(): void {
    void this.sessionService.getUserId().then((id) => {
      this.currentUserId.set(id);
      if (id) {
        this.joinForm.patchValue({ studentId: id });
      }
    });
  }

  createExam(): void {
    if (this.createExamForm.invalid || !this.currentUserId()) {
      this.createExamError.set(!this.currentUserId()
        ? 'Necesitas iniciar sesión primero (falta el userId del token).'
        : 'Revisa los campos.');
      return;
    }
    this.createExamError.set(null);
    const { title, description } = this.createExamForm.getRawValue();
    this.createRemoteExamUseCase.execute(title!, description || undefined, this.currentUserId()!).subscribe({
      next: (exam) => {
        this.createExamResult.set(exam);
        // precarga el examId en el siguiente paso para no copiar/pegar
        this.accessCodeForm.patchValue({ examId: exam.id });
      },
      error: (err: HttpErrorResponse) => this.createExamError.set(this.formatError(err)),
    });
  }

  generateAccessCode(): void {
    if (this.accessCodeForm.invalid) {
      this.accessCodeError.set('Revisa los campos.');
      return;
    }
    this.accessCodeError.set(null);
    const { examId, expiresInMinutes, code } = this.accessCodeForm.getRawValue();
    this.generateAccessCodeUseCase.execute(examId!, expiresInMinutes!, code || undefined).subscribe({
      next: (result) => {
        this.accessCodeResult.set(result);
        this.joinForm.patchValue({ examId: examId!, code: result.code });
      },
      error: (err: HttpErrorResponse) => this.accessCodeError.set(this.formatError(err)),
    });
  }

  joinExam(): void {
    if (this.joinForm.invalid) {
      this.joinError.set('Revisa los campos.');
      return;
    }
    this.joinError.set(null);
    const { examId, code, studentId } = this.joinForm.getRawValue();
    this.joinRemoteExamUseCase.execute(examId!, code!, studentId!).subscribe({
      next: (attempt) => {
        this.desktopRequired.set(false);
        this.joinResult.set(attempt);
        this.answerForm.patchValue({ attemptId: attempt.id });
        this.submitForm.patchValue({ attemptId: attempt.id });
      },
      error: (err: HttpErrorResponse) => {
        // Spec 51: el backend exige la app de escritorio para este examen.
        this.desktopRequired.set(err.error?.error === 'DESKTOP_APP_REQUIRED');
        this.joinError.set(this.formatError(err));
      },
    });
  }

  submitAnswer(): void {
    if (this.answerForm.invalid) {
      this.answerError.set('Revisa los campos.');
      return;
    }
    this.answerError.set(null);
    const { attemptId, questionId, selectedOptionIds, openTextAnswer } = this.answerForm.getRawValue();
    const optionIds = (selectedOptionIds || '')
      .split(',')
      .map((id) => id.trim())
      .filter((id) => id.length > 0);

    this.submitRemoteAnswerUseCase
      .execute(attemptId!, questionId!, optionIds, openTextAnswer || undefined)
      .subscribe({
        next: (answer) => this.answerResult.set(answer),
        error: (err: HttpErrorResponse) => this.answerError.set(this.formatError(err)),
      });
  }

  submitAttempt(): void {
    if (this.submitForm.invalid) {
      this.submitError.set('Revisa el identificador del intento (attemptId).');
      return;
    }
    this.submitError.set(null);
    const { attemptId } = this.submitForm.getRawValue();
    this.submitRemoteAttemptUseCase.execute(attemptId!).subscribe({
      next: (attempt) => this.submitResult.set(attempt),
      error: (err: HttpErrorResponse) => this.submitError.set(this.formatError(err)),
    });
  }

  private formatError(err: HttpErrorResponse): string {
    const body = err.error as { message?: string } | undefined;
    return body?.message ?? `Error ${err.status}: ${err.statusText}`;
  }
}
