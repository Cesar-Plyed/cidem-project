import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthRepository } from '../../domain/ports/auth.repository';
import { RegisterData } from '../../domain/models/register-data.model';
import { User } from '../../domain/models/user.model';

/**
 * Qué pasa cuando el usuario se registra. No inicia sesión automáticamente:
 * el backend solo crea la cuenta, el usuario hace login por separado.
 */
@Injectable({ providedIn: 'root' })
export class RegisterUseCase {
  constructor(private readonly authRepository: AuthRepository) {}

  execute(data: RegisterData): Observable<User> {
    return this.authRepository.register(data);
  }
}
