import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { TokenStorageService } from '../../infrastructure/storage/token-storage.service';
import { AuthTokens } from '../../domain/models/auth-tokens.model';
import { Role } from '../../domain/models/role.enum';

interface AccessTokenPayload {
  sub: string;
  email: string;
  role: Role;
  iat: number;
  exp: number;
}

/**
 * Orquesta el estado de sesión: guarda los tokens y expone el rol actual
 * de forma reactiva (para guards, menús, etc.).
 *
 * IMPORTANTE: el rol que se decodifica aquí es SOLO para UX (mostrar u
 * ocultar botones/rutas). Cualquiera puede editar un JWT en el navegador
 * antes de que lo leamos, así que esto nunca protege datos reales — esa
 * validación ocurre en el backend, que sí verifica la firma.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly roleSubject = new BehaviorSubject<Role | null>(null);
  readonly role$: Observable<Role | null> = this.roleSubject.asObservable();

  constructor(private readonly tokenStorage: TokenStorageService) {}

  async persistSession(tokens: AuthTokens): Promise<void> {
    await this.tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken);
    this.roleSubject.next(this.decodeRole(tokens.accessToken));
  }

  async clearSession(): Promise<void> {
    await this.tokenStorage.clear();
    this.roleSubject.next(null);
  }

  async isAuthenticated(): Promise<boolean> {
    const token = await this.tokenStorage.getAccessToken();
    if (!token) return false;
    return !this.isExpired(token);
  }

  async getRole(): Promise<Role | null> {
    const token = await this.tokenStorage.getAccessToken();
    if (!token) return null;
    return this.decodeRole(token);
  }

  async getUserId(): Promise<string | null> {
    const token = await this.tokenStorage.getAccessToken();
    if (!token) return null;
    try {
      return jwtDecode<AccessTokenPayload>(token).sub;
    } catch {
      return null;
    }
  }

  getRefreshToken(): Promise<string | null> {
    return this.tokenStorage.getRefreshToken();
  }

  private decodeRole(accessToken: string): Role | null {
    try {
      return jwtDecode<AccessTokenPayload>(accessToken).role;
    } catch {
      return null;
    }
  }

  private isExpired(token: string): boolean {
    try {
      const payload = jwtDecode<AccessTokenPayload>(token);
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true; // token corrupto o ilegible => tratarlo como expirado
    }
  }
}
