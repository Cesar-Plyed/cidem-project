import { Component, inject, input, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonHeader,
  IonToolbar,
} from '@ionic/angular';
import { SessionService } from '../../../application/session/session.service';
import { LogoutUseCase } from '../../../application/use-cases/logout.use-case';
import { Role } from '../../../domain/models/role.enum';

const ROLE_LABELS: Record<Role, string> = {
  [Role.ADMINISTRATIVO]: 'Administrativo',
  [Role.PROFESOR]: 'Profesor',
  [Role.ALUMNO]: 'Alumno',
};

/**
 * Barra superior compartida por todas las páginas autenticadas: marca,
 * título de la página, rol del usuario y cierre de sesión. Centraliza lo
 * que antes se repetía en el `ion-toolbar` de cada página.
 */
@Component({
  selector: 'app-header',
  templateUrl: 'app-header.component.html',
  styleUrls: ['app-header.component.scss'],
  imports: [RouterLink, IonHeader, IonToolbar, IonButtons, IonButton, IonBackButton],
})
export class AppHeaderComponent implements OnInit {
  private readonly sessionService = inject(SessionService);
  private readonly logoutUseCase = inject(LogoutUseCase);
  private readonly router = inject(Router);

  /** Título de la página actual (se muestra junto a la marca). */
  readonly title = input.required<string>();

  /** Ruta a la que regresa el botón atrás; si es null no se muestra. */
  readonly backHref = input<string | null>(null);

  readonly roleLabel = signal<string | null>(null);

  // ngOnInit debe devolver void: la sesión se consulta en un método aparte.
  ngOnInit(): void {
    void this.loadRoleLabel();
  }

  private async loadRoleLabel(): Promise<void> {
    const { role } = await this.sessionService.getSession();
    this.roleLabel.set(role ? ROLE_LABELS[role] : null);
  }

  async logout(): Promise<void> {
    await this.logoutUseCase.execute();
    await this.router.navigateByUrl('/login', { replaceUrl: true });
  }
}
