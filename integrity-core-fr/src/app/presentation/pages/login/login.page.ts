import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { IonContent } from '@ionic/angular';
import { LoginUseCase } from '../../../application/use-cases/login.use-case';
import { ApiError } from '../../../domain/models/api-error.model';

@Component({
  selector: 'app-login',
  templateUrl: 'login.page.html',
  styleUrls: ['login.page.scss'],
  imports: [ReactiveFormsModule, RouterLink, IonContent],
})
export class LoginPage {
  private readonly fb = inject(FormBuilder)
  private readonly loginUseCase = inject(LoginUseCase)
  private readonly router = inject(Router)

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  // Señales: la app es zoneless, así que mutar campos planos dentro de
  // subscribe() no repinta la vista (el error de HTTP nunca se mostraba).
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.getRawValue();

    this.loginUseCase.execute({ email: email!, password: password! }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/home']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const body = err.error as ApiError | undefined;
        // 401 -> "Correo o contraseña inválidos" (mensaje genérico del backend, no lo cambies)
        this.errorMessage.set(body?.message ?? 'Ocurrió un error. Intenta de nuevo.');
      },
    });
  }

  get email() {
    return this.form.controls.email;
  }

  get password() {
    return this.form.controls.password;
  }
}
