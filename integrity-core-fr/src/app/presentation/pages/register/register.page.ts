import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { IonContent } from '@ionic/angular';
import { RegisterUseCase } from '../../../application/use-cases/register.use-case';
import { ListCountriesUseCase } from '../../../application/use-cases/list-countries.use-case';
import { Country } from '../../../domain/models/country.model';
import { Role } from '../../../domain/models/role.enum';
import { ApiError } from '../../../domain/models/api-error.model';

@Component({
  selector: 'app-register',
  templateUrl: 'register.page.html',
  styleUrls: ['register.page.scss'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink, IonContent],
})
export class RegisterPage implements OnInit {
  private readonly fb = inject(FormBuilder)
  private readonly registerUseCase = inject(RegisterUseCase)
  private readonly listCountriesUseCase = inject(ListCountriesUseCase)
  private readonly router = inject(Router)

  readonly Role = Role;

  readonly form = this.fb.group({
    firstNames: ['', Validators.required],
    lastNames: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    countryId: ['', Validators.required],
    phoneNumber: [''],
    role: [Role.ALUMNO, Validators.required],
    registrationCode: [''],
  });

  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  countries: Country[] = [];

  constructor(

  ) {
    // El código de registro solo es obligatorio para PROFESOR/ADMINISTRATIVO.
    // Se revalida cada vez que el usuario cambia el rol.
    this.form.controls.role.valueChanges.subscribe(() => {
      const control = this.form.controls.registrationCode;
      control.setValidators(this.needsRegistrationCode ? [Validators.required] : []);
      control.updateValueAndValidity();
    });
  }

  ngOnInit(): void {
    this.listCountriesUseCase.execute().subscribe({
      next: (countries) => (this.countries = countries),
      error: () => (this.errorMessage = 'No se pudo cargar el catálogo de países.'),
    });
  }

  get needsRegistrationCode(): boolean {
    const role = this.form.controls.role.value;
    return role === Role.PROFESOR || role === Role.ADMINISTRATIVO;
  }

  get firstNames() {
    return this.form.controls.firstNames;
  }
  get lastNames() {
    return this.form.controls.lastNames;
  }
  get email() {
    return this.form.controls.email;
  }
  get password() {
    return this.form.controls.password;
  }
  get countryId() {
    return this.form.controls.countryId;
  }
  get registrationCode() {
    return this.form.controls.registrationCode;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const raw = this.form.getRawValue();

    this.registerUseCase
      .execute({
        firstNames: raw.firstNames!,
        lastNames: raw.lastNames!,
        email: raw.email!,
        password: raw.password!,
        countryId: raw.countryId!,
        phoneNumber: raw.phoneNumber || undefined,
        role: raw.role!,
        registrationCode: this.needsRegistrationCode ? raw.registrationCode || undefined : undefined,
      })
      .subscribe({
        next: () => {
          this.loading = false;
          this.successMessage = 'Cuenta creada. Ahora puedes iniciar sesión.';
          setTimeout(() => this.router.navigate(['/login']), 1200);
        },
        error: (err: HttpErrorResponse) => {
          this.loading = false;
          const body = err.error as ApiError | undefined;
          // El mensaje del backend ya viene listo para mostrar al usuario
          // (ej. "Codigo de registro invalido o faltante para el rol PROFESOR").
          this.errorMessage = this.formatError(body);
        },
      });
  }

  private formatError(body: ApiError | undefined): string {
    if (!body) return 'Ocurrió un error. Intenta de nuevo.';

    if (body.fieldErrors) {
      const firstFieldError = Object.values(body.fieldErrors)[0];
      if (firstFieldError) return firstFieldError;
    }

    return body.message ?? 'Ocurrió un error. Intenta de nuevo.';
  }
}
