import { inject, Injectable } from '@angular/core';
import { firstValueFrom, timeout, catchError } from 'rxjs';
import { AuthRepository } from '../../domain/ports/auth.repository';
import { SessionService } from '../session/session.service';

/**
 * Qué pasa cuando el usuario cierra sesión: avisa al backend (para que
 * invalide el refresh token) y limpia la sesión local. Si la llamada al
 * backend falla (sin red, etc.), igual limpiamos localmente — no tiene
 * sentido dejar al usuario "atrapado" en una sesión que ya no quiere.
 */
@Injectable({ providedIn: 'root' })
export class LogoutUseCase {
  private readonly authRepository = inject(AuthRepository)
  private readonly sessionService = inject(SessionService)
  
  async execute(): Promise<void> {
    const refreshToken = await this.sessionService.getRefreshToken();
    if (refreshToken) {
      try {
        await firstValueFrom(
          this.authRepository.logout(refreshToken).pipe(
            timeout(5000),
            catchError(() => []),
          ),
        );
      } catch {
        // Silently ignore timeout or network errors — session is cleared below regardless
      }
    }
    await this.sessionService.clearSession();
  }
}
