import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { concatMap, finalize, from, switchMap, toArray } from 'rxjs';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonRadioGroup,
  IonRadio,
  IonCheckbox,
  IonItem,
  IonTextarea,
  IonButton,
  IonText,
  IonSpinner,
  IonBadge,
  AlertController,
} from '@ionic/angular';
import { GetAttemptExamUseCase } from '../../../application/use-cases/evaluator/get-attempt-exam.use-case';
import { SubmitRemoteAnswerUseCase } from '../../../application/use-cases/evaluator/submit-remote-answer.use-case';
import { SubmitRemoteAttemptUseCase } from '../../../application/use-cases/evaluator/submit-remote-attempt.use-case';
import { AttemptExam, AttemptExamQuestion } from '../../../domain/models/evaluator/attempt-exam.model';

/**
 * Vista que se abre al pulsar "Comenzar examen": carga el examen del intento del
 * alumno, deja responder y, al final, guarda las respuestas y entrega el intento.
 */
@Component({
  selector: 'app-exam-attempt',
  templateUrl: 'exam-attempt.page.html',
  styleUrls: ['exam-attempt.page.scss'],
  imports: [
    RouterLink,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardContent,
    IonRadioGroup,
    IonRadio,
    IonCheckbox,
    IonItem,
    IonTextarea,
    IonButton,
    IonText,
    IonSpinner,
    IonBadge,
  ],
})
export class ExamAttemptPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly alertController = inject(AlertController);
  private readonly getAttemptExamUseCase = inject(GetAttemptExamUseCase);
  private readonly submitAnswerUseCase = inject(SubmitRemoteAnswerUseCase);
  private readonly submitAttemptUseCase = inject(SubmitRemoteAttemptUseCase);

  // Signals: sin zone.js, Angular solo refresca la vista cuando cambia un signal.
  readonly exam = signal<AttemptExam | null>(null);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly submitted = signal(false);
  readonly errorMessage = signal<string | null>(null);

  /** Opciones elegidas por pregunta (radio = 1 elemento, checkbox = varios). */
  readonly selected = signal<Record<string, string[]>>({});
  /** Texto de las preguntas abiertas. */
  readonly texts = signal<Record<string, string | undefined>>({});

  readonly answeredCount = computed(() => {
    const exam = this.exam();
    if (!exam) return 0;
    return exam.questions.filter((question) => this.isAnswered(question)).length;
  });

  private confirming = false;

  ngOnInit(): void {
    const attemptId = this.route.snapshot.paramMap.get('attemptId');
    if (!attemptId) {
      this.errorMessage.set('No se encontró el intento del examen.');
      this.loading.set(false);
      return;
    }

    this.getAttemptExamUseCase.execute(attemptId).subscribe({
      next: (exam) => {
        this.restoreAnswers(exam);
        this.exam.set(exam);
        this.submitted.set(exam.submitted);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar el examen', err.status, err.error);
        this.errorMessage.set(this.messageOf(err, 'No se pudo cargar el examen'));
        this.loading.set(false);
      },
    });
  }

  isSelected(questionId: string, optionId: string): boolean {
    return (this.selected()[questionId] ?? []).includes(optionId);
  }

  selectSingle(questionId: string, optionId: string | null | undefined): void {
    if (!optionId) return;
    this.selected.update((current) => ({ ...current, [questionId]: [optionId] }));
  }

  toggleOption(questionId: string, optionId: string, checked: boolean): void {
    this.selected.update((current) => {
      const chosen = new Set(current[questionId] ?? []);
      if (checked) chosen.add(optionId);
      else chosen.delete(optionId);
      return { ...current, [questionId]: [...chosen] };
    });
  }

  setText(questionId: string, value: string | null | undefined): void {
    this.texts.update((current) => ({ ...current, [questionId]: value ?? '' }));
  }

  /** Pide confirmación. Ignora clics repetidos mientras hay un aviso abierto o se está enviando. */
  async confirmSubmit(): Promise<void> {
    const exam = this.exam();
    if (!exam || this.confirming || this.submitting() || this.submitted()) return;
    this.confirming = true;

    const pending = exam.questions.length - this.answeredCount();
    const warning =
      pending > 0 ? `Tienes ${pending} pregunta(s) sin responder. ` : '';
    const alert = await this.alertController.create({
      header: 'Enviar examen',
      message: `${warning}Una vez enviado no podrás cambiar tus respuestas. ¿Enviar el examen?`,
      buttons: [
        { text: 'Revisar', role: 'cancel' },
        { text: 'Enviar', handler: () => this.submit() },
      ],
    });
    await alert.present();
    await alert.onDidDismiss();
    this.confirming = false;
  }

  private submit(): void {
    const exam = this.exam();
    if (!exam || this.submitting() || this.submitted()) return;

    this.submitting.set(true);
    this.errorMessage.set(null);

    const answered = exam.questions.filter((question) => this.isAnswered(question));

    // Primero guarda cada respuesta (una por una) y, si todo salió bien, entrega el intento.
    from(answered)
      .pipe(
        concatMap((question) =>
          question.type === 'OPEN_ENDED'
            ? this.submitAnswerUseCase.execute(
                exam.attemptId,
                question.id,
                [],
                (this.texts()[question.id] ?? '').trim(),
              )
            : this.submitAnswerUseCase.execute(exam.attemptId, question.id, this.selected()[question.id] ?? []),
        ),
        toArray(),
        switchMap(() => this.submitAttemptUseCase.execute(exam.attemptId)),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: () => this.submitted.set(true),
        error: (err: HttpErrorResponse) => {
          console.error('Error al enviar el examen', err.status, err.error);
          this.errorMessage.set(this.messageOf(err, 'No se pudo enviar el examen. Intenta de nuevo'));
        },
      });
  }

  private isAnswered(question: AttemptExamQuestion): boolean {
    if (question.type === 'OPEN_ENDED') {
      return (this.texts()[question.id] ?? '').trim().length > 0;
    }
    return (this.selected()[question.id] ?? []).length > 0;
  }

  /** Recupera lo ya respondido (por ejemplo, si el alumno recargó la página). */
  private restoreAnswers(exam: AttemptExam): void {
    const selected: Record<string, string[]> = {};
    const texts: Record<string, string | undefined> = {};
    for (const answer of exam.answers ?? []) {
      if (answer.selectedOptionIds?.length) selected[answer.questionId] = [...answer.selectedOptionIds];
      if (answer.openTextAnswer) texts[answer.questionId] = answer.openTextAnswer;
    }
    this.selected.set(selected);
    this.texts.set(texts);
  }

  private messageOf(err: HttpErrorResponse, fallback: string): string {
    const detail = (err.error as { message?: string } | null)?.message;
    return `${fallback} (HTTP ${err.status}).${detail ? ' ' + detail : ''}`;
  }
}
