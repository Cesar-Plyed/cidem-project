import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthRepository } from '../../domain/ports/auth.repository';
import { AuthCredentials } from '../../domain/models/auth-credentials.model';
import { RegisterData } from '../../domain/models/register-data.model';
import { AuthTokens } from '../../domain/models/auth-tokens.model';
import { User } from '../../domain/models/user.model';

/**
 * Adaptador de salida: implementa el puerto AuthRepository contra el
 * backend real vía HTTP. Esta es la única clase que sabe que existe
 * un endpoint /auth/login, /auth/register, etc.
 */
@Injectable({ providedIn: 'root' })
export class AuthHttpRepository extends AuthRepository {
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  constructor(private readonly http: HttpClient) {
    super();
  }

  login(credentials: AuthCredentials): Observable<AuthTokens> {
    return this.http.post<AuthTokens>(`${this.baseUrl}/login`, credentials);
  }

  register(data: RegisterData): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/register`, data);
  }

  refresh(refreshToken: string): Observable<AuthTokens> {
    return this.http.post<AuthTokens>(`${this.baseUrl}/refresh`, { refreshToken });
  }

  logout(refreshToken: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, { refreshToken });
  }
}
