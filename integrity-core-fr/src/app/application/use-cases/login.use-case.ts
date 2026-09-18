import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthRepository } from '../../domain/ports/auth.repository';
import { AuthCredentials } from '../../domain/models/auth-credentials.model';
import { AuthTokens } from '../../domain/models/auth-tokens.model';
import { SessionService } from '../session/session.service';

/**
 * Qué pasa cuando el usuario intenta iniciar sesión: le pide los tokens
 * al backend (a través del puerto) y, si tiene éxito, persiste la sesión.
 */
@Injectable({ providedIn: 'root' })
export class LoginUseCase {
  constructor(
    private readonly authRepository: AuthRepository,
    private readonly sessionService: SessionService,
  ) {}

  execute(credentials: AuthCredentials): Observable<AuthTokens> {
    return this.authRepository
      .login(credentials)
      .pipe(tap((tokens) => this.sessionService.persistSession(tokens)));
  }
}
