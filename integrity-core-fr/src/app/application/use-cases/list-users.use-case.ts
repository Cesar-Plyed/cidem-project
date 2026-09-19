import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthRepository } from '../../domain/ports/auth.repository';
import { DirectoryUser } from '../../domain/models/user-directory.model';

@Injectable({ providedIn: 'root' })
export class ListUsersUseCase {
  private readonly authRepository = inject(AuthRepository)

  execute(): Observable<DirectoryUser[]> {
    return this.authRepository.getUsers();
  }
}
