import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Role } from '../domain/models/role.enum';
import { SessionService } from '../application/session/session.service';
import { JoinRemoteExamUseCase } from '../application/use-cases/evaluator/join-remote-exam.use-case';
import { ListUsersUseCase } from '../application/use-cases/list-users.use-case';
import { DirectoryUser } from '../domain/models/user-directory.model';
import { AppHeaderComponent } from '../presentation/components/app-header/app-header.component';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
  imports: [CommonModule, RouterLink, ReactiveFormsModule, IonContent, AppHeaderComponent],
})
export class HomePage implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly sessionService = inject(SessionService);
  private readonly joinRemoteExamUseCase = inject(JoinRemoteExamUseCase);
  private readonly listUsersUseCase = inject(ListUsersUseCase);
  private readonly router = inject(Router);


  readonly Role = Role;

  // Signals: la app corre sin zone.js (zoneless), así que Angular solo
  // refresca la vista cuando cambia un signal (o un evento del template).
  readonly role = signal<Role | null>(null);
  readonly userId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly joining = signal(false);
  readonly users = signal<DirectoryUser[]>([]);
  readonly loading = signal(true); // resolviendo la sesión (rápido, local)
  readonly usersLoading = signal(false); // pidiendo el directorio al backend

  readonly joinForm = this.fb.group({
    code: ['', Validators.required],
  });

  // ngOnInit debe devolver void: la sesión se lee en un método aparte y el
  // resultado se escribe en signals (la app es zoneless).
  ngOnInit(): void {
    void this.loadSession();
  }

  private async loadSession(): Promise<void> {
    const { role, userId } = await this.sessionService.getSession();
    this.role.set(role);
    this.userId.set(userId);
    // El panel ya se puede pintar: no depende del backend.
    this.loading.set(false);

    if (role !== Role.ADMINISTRATIVO && role !== Role.PROFESOR) return;

    // El directorio se carga aparte, sin bloquear el resto del panel.
    this.usersLoading.set(true);
    this.listUsersUseCase.execute().subscribe({
      next: (users) => this.users.set(users),
      error: () => {
        this.errorMessage.set('No se pudo cargar el directorio de usuarios.');
        this.usersLoading.set(false);
      },
      complete: () => this.usersLoading.set(false),
    });
  }

  joinExam(): void {
    // Un solo ingreso a la vez: la guarda es síncrona, así que ni varios clics
    // seguidos ni Enter repetido pueden lanzar más de una petición.
    if (this.joining()) return;

    if (this.joinForm.invalid || !this.userId()) {
      this.joinForm.markAllAsTouched();
      this.errorMessage.set(
        !this.userId() ? 'Tu sesión no tiene un identificador válido.' : 'Ingresa el código de acceso.',
      );
      return;
    }

    this.errorMessage.set(null);
    this.joining.set(true);
    // Los códigos se guardan en mayúsculas: se normaliza lo que escribe el alumno.
    const code = this.joinForm.getRawValue().code!.trim().toUpperCase();

    this.joinRemoteExamUseCase.executeByCode(code).subscribe({
      next: (attempt) => {
        // Se mantiene "joining" hasta que la navegación termina, para no aceptar otro clic en medio.
        void this.router.navigate(['/attempts', attempt.id]).finally(() => this.joining.set(false));
      },
      error: (err: HttpErrorResponse) => {
        this.joining.set(false);
        this.errorMessage.set(
          (err.error as { message?: string } | undefined)?.message ?? 'No fue posible unirse al examen.',
        );
      },
    });
  }
}
