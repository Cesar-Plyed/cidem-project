import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { SessionService } from '../../application/session/session.service';
import { Role } from '../../domain/models/role.enum';

/**
 * Uso en rutas: canActivate: [roleGuard([Role.PROFESOR])]
 * Es un guard de UX (evita mostrar pantallas que no le corresponden al
 * usuario). La protección real de datos sigue siendo el backend.
 */
export function roleGuard(allowedRoles: Role[]): CanActivateFn {
  return async () => {
    const sessionService = inject(SessionService);
    const router = inject(Router);

    const session = await sessionService.getSession();
    if (!session.role || !session.userId) {
      router.navigate(['/login']);
      return false;
    }

    if (!allowedRoles.includes(session.role)) {
      router.navigate(['/home']);
      return false;
    }

    return true;
  };
}
