import { inject, Injectable } from '@angular/core';
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

export interface SessionSnapshot {
  role: Role | null;
  userId: string | null;
}

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly tokenStorage = inject(TokenStorageService);

  private readonly roleSubject = new BehaviorSubject<Role | null>(null);
  private currentSession: SessionSnapshot = { role: null, userId: null };
  readonly role$: Observable<Role | null> = this.roleSubject.asObservable();

  async persistSession(tokens: AuthTokens): Promise<void> {
    await this.tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken);
    this.currentSession = this.decodeSession(tokens.accessToken);
    this.roleSubject.next(this.currentSession.role);
  }

  async clearSession(): Promise<void> {
    await this.tokenStorage.clear();
    this.currentSession = { role: null, userId: null };
    this.roleSubject.next(null);
  }

  async getSession(): Promise<SessionSnapshot> {
    if (this.currentSession.role && this.currentSession.userId) {
      return this.currentSession;
    }

    const token = await this.tokenStorage.getAccessToken();
    if (!token || this.isExpired(token)) {
      this.currentSession = { role: null, userId: null };
      return this.currentSession;
    }

    this.currentSession = this.decodeSession(token);
    this.roleSubject.next(this.currentSession.role);
    return this.currentSession;
  }

  async isAuthenticated(): Promise<boolean> {
    const session = await this.getSession();
    return session.role !== null && session.userId !== null;
  }

  async getRole(): Promise<Role | null> {
    return (await this.getSession()).role;
  }

  async getUserId(): Promise<string | null> {
    return (await this.getSession()).userId;
  }

  getRefreshToken(): Promise<string | null> {
    return this.tokenStorage.getRefreshToken();
  }

  private decodeSession(accessToken: string): SessionSnapshot {
    try {
      const payload = jwtDecode<AccessTokenPayload>(accessToken);
      return { role: payload.role, userId: payload.sub };
    } catch {
      return { role: null, userId: null };
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
