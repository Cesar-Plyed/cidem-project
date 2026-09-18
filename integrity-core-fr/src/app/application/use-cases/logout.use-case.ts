import { Injectable } from '@angular/core';
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
  constructor(
    private readonly authRepository: AuthRepository,
    private readonly sessionService: SessionService,
  ) {}

  async execute(): Promise<void> {
    const refreshToken = await this.sessionService.getRefreshToken();
    if (refreshToken) {
      this.authRepository.logout(refreshToken).subscribe({ error: () => {} });
    }
    await this.sessionService.clearSession();
  }
}
